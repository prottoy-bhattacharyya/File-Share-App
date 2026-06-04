FROM python:3.12-slim-bookworm

ENV PYTHONUNBUFFERED=1

RUN pip install uv

WORKDIR /app

COPY . .

EXPOSE 8000

RUN pip install -r requirements.txt

CMD ["python3", "manage.py", "runserver", "0.0.0.0:8000"]
# CMD [ "python3", "-m", "uv", "run", "manage.py", "runserver", "0.0.0.0:8000" ]
