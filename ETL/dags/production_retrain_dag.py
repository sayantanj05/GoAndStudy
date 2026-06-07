"""Production retraining DAG for all recommendation models."""
from datetime import datetime, timedelta
from airflow import DAG
from airflow.operators.bash import BashOperator
from airflow.operators.python import PythonOperator

import os

ML_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), 'ML')

default_args = {
    'owner': 'ml-team',
    'depends_on_past': False,
    'email_on_failure': True,
    'email': ['ml-team@goandstudy.com'],
    'retries': 2,
    'retry_delay': timedelta(minutes=10),
}

with DAG(
    'production_ml_retrain',
    default_args=default_args,
    description='Retrain all recommendation models in production order',
    schedule_interval='0 2 * * 0',  # Weekly on Sunday 2 AM
    start_date=datetime(2024, 1, 1),
    catchup=False,
    tags=['ml', 'production', 'recommendations'],
) as dag:
    
    # Step 1: Ensure ETL is fresh
    verify_etl = BashOperator(
        task_id='verify_clickhouse_etl',
        bash_command=f'cd {ML_DIR} && python scripts/data_loader.py',
    )
    
    # Step 2: Train Two-Tower embeddings (Phase 4)
    train_two_tower = BashOperator(
        task_id='train_two_tower',
        bash_command=f'cd {ML_DIR} && python scripts/train_two_tower.py',
    )
    
    # Step 3: Build FAISS index with Two-Tower embeddings
    build_faiss = BashOperator(
        task_id='build_faiss_index',
        bash_command=f'cd {ML_DIR} && python scripts/build_faiss_index.py',
    )
    
    # Step 4: Train XGBoost LambdaRank (Phase 3)
    train_ranker = BashOperator(
        task_id='train_xgboost_ranker',
        bash_command=f'cd {ML_DIR} && python scripts/train_xgboost_ranker.py --candidates 200',
    )
    
    # Step 5: Train baseline recommender (fallback)
    train_baseline = BashOperator(
        task_id='train_baseline_recommender',
        bash_command=f'cd {ML_DIR} && python scripts/train_recommender.py',
    )
    
    # Step 6: Refresh feature store
    refresh_features = BashOperator(
        task_id='refresh_feature_store',
        bash_command=f'cd {ML_DIR} && python -c "from services.feature_store import RedisFeatureStore; s=RedisFeatureStore(); print(\"Store ready\")"',
    )
    
    # Step 7: Model validation check
    def validate_models():
        import os
        required_files = [
            'ML/models/faiss/twotower_index.faiss',
            'ML/models/xgboost_ranker/ranker.json',
            'ML/models/recommender/best_model.joblib',
        ]
        missing = [f for f in required_files if not os.path.exists(f)]
        if missing:
            raise ValueError(f"Missing models: {missing}")
        print("All models validated successfully")
    
    validate = PythonOperator(
        task_id='validate_models',
        python_callable=validate_models,
    )
    
    # DAG structure
    verify_etl >> [train_two_tower, train_ranker, train_baseline]
    train_two_tower >> build_faiss
    [build_faiss, train_ranker, train_baseline] >> refresh_features >> validate

