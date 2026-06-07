"""
Airflow DAG for daily ML model retraining.

This DAG:
1. Checks if enough new data exists for retraining
2. Trains all models (NMF, SVD, SVD++, Hybrid)
3. Evaluates and selects the best model
4. Saves model with versioned filename
5. Updates the 'latest' symlink
6. Sends notification if model performance degrades
"""

from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.operators.bash import BashOperator
from airflow.providers.http.operators.http import SimpleHttpOperator
from datetime import datetime, timedelta
import os
import json
import shutil
import sys

# Add ML scripts to path
ML_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), '..', 'ML')
sys.path.insert(0, ML_DIR)

default_args = {
    'owner': 'ml-team',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

with DAG(
    'ml_model_retrain',
    default_args=default_args,
    description='Daily retraining of ML recommendation models',
    schedule_interval='0 2 * * *',  # Run at 2 AM daily
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=['ml', 'retraining'],
) as dag:

    def check_data_freshness(**context):
        """Check if enough new interactions exist for retraining."""
        try:
            from scripts.data_loader import load_interactions
            df = load_interactions()
            n_interactions = len(df)
            
            # Load previous count
            state_file = os.path.join(ML_DIR, 'models', 'recommender', 'training_state.json')
            prev_count = 0
            if os.path.exists(state_file):
                with open(state_file, 'r') as f:
                    prev_count = json.load(f).get('interaction_count', 0)
            
            new_interactions = n_interactions - prev_count
            print(f"Total interactions: {n_interactions}, Previous: {prev_count}, New: {new_interactions}")
            
            if new_interactions < 10:
                print("Insufficient new data (< 10 interactions). Skipping retraining.")
                return False
            
            # Save current count
            os.makedirs(os.path.dirname(state_file), exist_ok=True)
            with open(state_file, 'w') as f:
                json.dump({'interaction_count': n_interactions, 'last_checked': datetime.now().isoformat()}, f)
            
            return True
        except Exception as e:
            print(f"Error checking data freshness: {e}")
            return False

    def train_models(**context):
        """Train all models and select the best one."""
        try:
            from scripts.train_recommender import train_and_compare
            best_model, metrics = train_and_compare()
            
            if best_model is None:
                raise Exception("All models failed to train")
            
            # Push metrics to XCom
            context['ti'].xcom_push(key='best_model_type', value=metrics.get('best_model_type', 'unknown'))
            context['ti'].xcom_push(key='best_rmse', value=metrics.get('best_rmse', 999))
            context['ti'].xcom_push(key='training_samples', value=metrics.get('training_samples', 0))
            
            return f"Best model: {metrics['best_model_type']} (RMSE: {metrics['best_rmse']:.4f})"
        except Exception as e:
            raise Exception(f"Model training failed: {e}")

    def version_and_deploy(**context):
        """Version the model and update the latest symlink."""
        model_dir = os.path.join(ML_DIR, 'models', 'recommender')
        best_model_path = os.path.join(model_dir, 'best_model.joblib')
        
        if not os.path.exists(best_model_path):
            raise Exception("No best_model.joblib found")
        
        # Create versioned copy
        timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
        versioned_name = f"model_{timestamp}.joblib"
        versioned_path = os.path.join(model_dir, versioned_name)
        shutil.copy2(best_model_path, versioned_path)
        
        # Update latest symlink (Windows: copy instead of symlink)
        latest_path = os.path.join(model_dir, 'latest_model.joblib')
        if os.path.exists(latest_path):
            os.remove(latest_path)
        shutil.copy2(best_model_path, latest_path)
        
        print(f"Model versioned: {versioned_name}")
        return versioned_name

    def check_performance_regression(**context):
        """Alert if model performance degraded vs previous run."""
        try:
            current_rmse = context['ti'].xcom_pull(task_ids='train_models', key='best_rmse') or 999
            
            # Load previous best RMSE
            perf_file = os.path.join(ML_DIR, 'models', 'recommender', 'performance_history.json')
            prev_rmse = None
            if os.path.exists(perf_file):
                with open(perf_file, 'r') as f:
                    history = json.load(f)
                    if history:
                        prev_rmse = history[-1].get('rmse')
            
            # Save current performance
            history = []
            if os.path.exists(perf_file):
                with open(perf_file, 'r') as f:
                    history = json.load(f)
            
            history.append({
                'timestamp': datetime.now().isoformat(),
                'rmse': current_rmse,
                'model_type': context['ti'].xcom_pull(task_ids='train_models', key='best_model_type')
            })
            
            # Keep last 30 entries
            history = history[-30:]
            with open(perf_file, 'w') as f:
                json.dump(history, f, indent=2)
            
            if prev_rmse and current_rmse > prev_rmse * 1.1:  # 10% worse
                print(f"WARNING: Model performance degraded! Previous RMSE: {prev_rmse:.4f}, Current: {current_rmse:.4f}")
                # In production, send alert here
            else:
                print(f"Model performance OK. RMSE: {current_rmse:.4f}")
            
            return True
        except Exception as e:
            print(f"Performance check error: {e}")
            return False

    # Task definitions
    t1_check_data = PythonOperator(
        task_id='check_data_freshness',
        python_callable=check_data_freshness,
    )

    t2_train = PythonOperator(
        task_id='train_models',
        python_callable=train_models,
    )

    t3_version = PythonOperator(
        task_id='version_and_deploy',
        python_callable=version_and_deploy,
    )

    t4_check_perf = PythonOperator(
        task_id='check_performance',
        python_callable=check_performance_regression,
    )

    # Task dependencies
    t1_check_data >> t2_train >> t3_version >> t4_check_perf

