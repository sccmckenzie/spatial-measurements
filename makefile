MAKEFLAGS += --warn-undefined-variables

.PHONY: provision

# Adjust container name if different
DB_CONTAINER_NAME=spatial-measurements-db-1

provision:
	@echo "Running docker compose"
	docker compose up -d
	@echo "Waiting for Postgres to be ready..."
	@until docker exec $(DB_CONTAINER_NAME) pg_isready -U postgres > /dev/null 2>&1; do \
		sleep 2; \
	done
	@echo "Postgres is ready."
	@echo "Generating grid file"
	@eval poetry run python provision-grid.py
	@echo "writing grid file to postgres config db"
	@eval "poetry run dbt seed --profile spatial_config --project-dir dbt_spatial_config"