@REM create requirements.txt
python -m uv pip compile pyproject.toml -o requirements.txt

@REM install dependencies
python -m uv add -r requirements.txt

@REM create django project
python -m uv run django-admin startproject django_api .
