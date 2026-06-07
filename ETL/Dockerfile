FROM apache/airflow:3.2.0

USER root
COPY requirements.txt /opt/airflow/requirements.txt
RUN chown -R airflow: /opt/airflow/requirements.txt
USER ${AIRFLOW_UID}

USER airflow
RUN pip install --no-cache-dir --user -r /opt/airflow/requirements.txt
