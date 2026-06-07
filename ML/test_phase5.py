"""Quick test for Phase 5 predictive model imports."""
import sys
print("Python path:", sys.path[:3])

try:
    from api.routers import predictions
    print("✅ Predictions router: OK")
except Exception as e:
    print(f"❌ Predictions router: {e}")

try:
    from scripts.predictive_models import get_churn_predictor, get_overdue_predictor, get_fine_estimator
    print("✅ Predictive models: OK")
except Exception as e:
    print(f"❌ Predictive models: {e}")

try:
    churn = get_churn_predictor()
    print(f"   ChurnPredictor created (model_trained={churn.model is not None})")
except Exception as e:
    print(f"   ChurnPredictor failed: {e}")

try:
    overdue = get_overdue_predictor()
    print(f"   OverduePredictor created (model_trained={overdue.model is not None})")
except Exception as e:
    print(f"   OverduePredictor failed: {e}")

try:
    fine = get_fine_estimator()
    print(f"   FineEstimator created (model_trained={fine.model is not None})")
except Exception as e:
    print(f"   FineEstimator failed: {e}")

print("\nPhase 5 smoke test complete!")
