"""
AirFlow DAG for weekly retraining of Phase 4 predictive models.
Retrains churn, overdue, and fine estimation models.
"""

from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.python import PythonOperator
import sys
import os

# Add ML scripts to path
sys.path.insert(0, '/opt/airflow/ML')

from scripts.predictive_models import get_churn_predictor, get_overdue_predictor, get_fine_estimator


default_args = {
    'owner': 'airflow',
    'depends_on_past': False,
    'email_on_failure': False,
    'email_on_retry': False,
    'retries': 1,
    'retry_delay': timedelta(minutes=5),
}

def retrain_churn_model():
    """Retrain churn prediction model."""
    print("=" * 50)
    print("RETRAINING CHURN MODEL")
    print("=" * 50)
    predictor = get_churn_predictor()
    success = predictor.train()
    if success:
        print("✅ Churn model retrained successfully")
    else:
        print("⚠️ Churn model retraining skipped (insufficient data)")
    return success

def retrain_overdue_model():
    """Retrain overdue prediction model."""
    print("=" * 50)
    print("RETRAINING OVERDUE MODEL")
    print("=" * 50)
    predictor = get_overdue_predictor()
    success = predictor.train()
    if success:
        print("✅ Overdue model retrained successfully")
    else:
        print("⚠️ Overdue model retraining skipped (insufficient data)")
    return success

def retrain_fine_model():
    """Retrain fine estimation model."""
    print("=" * 50)
    print("RETRAINING FINE MODEL")
    print("=" * 50)
    predictor = get_fine_estimator()
    success = predictor.train()
    if success:
        print("✅ Fine model retrained successfully")
    else:
        print("⚠️ Fine model retraining skipped (insufficient data)")
    return success

def models_health_check():
    """Verify all models are ready."""
    churn = get_churn_predictor()
    overdue = get_overdue_predictor()
    fine = get_fine_estimator()
    
    status = {
        'churn': churn.model is not None,
        'overdue': overdue.model is not None,
        'fine': fine.model is not None,
        'timestamp': datetime.now().isoformat()
    }
    
    print(f"Model health: {status}")
    return status


with DAG(
    'retrain_predictive_models',
    default_args=default_args,
    description='Weekly retraining of churn, overdue, and fine prediction models',
    schedule_interval='0 2 * * 0',  # Every Sunday at 2 AM
    start_date=datetime(2026, 1, 1),
    catchup=False,
    tags=['ml', 'prediction', 'phase5'],
) as dag:

    health_check = PythonOperator(
        task_id='health_check',
        python_callable=models_health_check,
    )

    retrain_churn = PythonOperator(
        task_id='retrain_churn',
        python_callable=retrain_churn_model,
    )

    retrain_overdue = PythonOperator(
        task_id='retrain_overdue',
        python_callable=retrain_overdue_model,
    )

    retrain_fine = PythonOperator(
        task_id='retrain_fine',
        python_callable=retrain_fine_model,
    )

    # Parallel retraining after health check
    health_check >> [retrain_churn, retrain_overdue, retrain_fine]
