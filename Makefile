.PHONY: help build up down restart logs status clean test gradle-build gradle-test

.DEFAULT_GOAL := help

GREEN := \033[32m
YELLOW := \033[33m
RED := \033[31m
RESET := \033[0m
help: ## Show this help message
	@echo "$(GREEN)Greg Fullstack Application - Available Commands:$(RESET)"
	@echo ""
	@awk 'BEGIN {FS = ":.*##"} /^[a-zA-Z_-]+:.*##/ { printf "  $(YELLOW)%-15s$(RESET) %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "$(GREEN)Quick Start:$(RESET)"
	@echo "  make up    - Start all services"
	@echo "  make down  - Stop all services"
	@echo "  make logs  - View application logs"

build: ## Build all Docker images
	@echo "$(GREEN)Building Docker images...$(RESET)"
	docker compose build

up: ## Start all services in detached mode
	@echo "$(GREEN)Starting Greg Fullstack services...$(RESET)"
	docker compose up --build -d
	@echo "$(GREEN)Services started successfully!$(RESET)"
	@echo "$(YELLOW)Application:$(RESET) http://localhost:8080"
	@echo "$(YELLOW)pgAdmin:$(RESET)     http://localhost:5050 (admin@admin.com / admin)"
	@echo "$(YELLOW)PostgreSQL:$(RESET)  localhost:5432"

up-dev: ## Start only database services for local development
	@echo "$(GREEN)Starting database services for local development...$(RESET)"
	docker compose -f docker-compose.dev.yml up -d
	@echo "$(GREEN)Database services started successfully!$(RESET)"
	@echo "$(YELLOW)PostgreSQL:$(RESET)  localhost:5432 (postgres/password)"
	@echo "$(YELLOW)pgAdmin:$(RESET)     http://localhost:5050 (admin@admin.com / admin)"
	@echo "$(GREEN)Now run your Spring Boot app through IntelliJ!$(RESET)"

down: ## Stop all services
	@echo "$(GREEN)Stopping Greg Fullstack services...$(RESET)"
	docker compose down
	@echo "$(GREEN)Services stopped successfully!$(RESET)"

down-dev: ## Stop only database services
	@echo "$(GREEN)Stopping database services...$(RESET)"
	docker compose -f docker-compose.dev.yml down
	@echo "$(GREEN)Database services stopped successfully!$(RESET)"

restart: down up ## Restart all services

logs: ## Show application logs (follow mode)
	@echo "$(GREEN)Showing application logs (Ctrl+C to stop):$(RESET)"
	docker compose logs -f app

logs-all: ## Show logs for all services
	@echo "$(GREEN)Showing all service logs (Ctrl+C to stop):$(RESET)"
	docker compose logs -f

logs-db: ## Show PostgreSQL logs
	@echo "$(GREEN)Showing PostgreSQL logs (Ctrl+C to stop):$(RESET)"
	docker compose logs -f postgres

status: ## Show service status
	@echo "$(GREEN)Service Status:$(RESET)"
	docker compose ps
db-shell: ## Connect to PostgreSQL database shell
	@echo "$(GREEN)Connecting to PostgreSQL database...$(RESET)"
	docker compose exec postgres psql -U postgres -d greg_fullstack

db-reset: ## Reset database (WARNING: This will delete all data!)
	@echo "$(RED)WARNING: This will delete all database data!$(RESET)"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ]
	docker compose down -v
	docker volume rm greg-fullstack_postgres_data 2>/dev/null || true
	@echo "$(GREEN)Database reset complete. Run 'make up' to start fresh.$(RESET)"

gradle-build: ## Build the application using Gradle (without Docker)
	@echo "$(GREEN)Building application with Gradle...$(RESET)"
	./gradlew clean build

gradle-test: ## Run tests using Gradle (without Docker)
	@echo "$(GREEN)Running tests with Gradle...$(RESET)"
	./gradlew test

gradle-bootrun: ## Run application locally using Gradle (requires local PostgreSQL)
	@echo "$(GREEN)Running application locally...$(RESET)"
	./gradlew bootRun
clean: ## Remove containers, networks, and volumes
	@echo "$(YELLOW)This will remove all containers, volumes, and images related to this project.$(RESET)"
	@read -p "Are you sure? (y/N): " confirm && [ "$$confirm" = "y" ]
	@echo "$(GREEN)Cleaning up Docker resources...$(RESET)"
	docker compose down -v --rmi all --remove-orphans 2>/dev/null || true
	docker system prune -f
	@echo "$(GREEN)Cleanup completed!$(RESET)"

clean-images: ## Remove only the project images
	@echo "$(GREEN)Removing project Docker images...$(RESET)"
	docker compose down --rmi all

clean-volumes: ## Remove only the project volumes
	@echo "$(GREEN)Removing project Docker volumes...$(RESET)"
	docker compose down -v
health: ## Check application health
	@echo "$(GREEN)Checking application health...$(RESET)"
	@curl -f http://localhost:8080/actuator/health 2>/dev/null || echo "$(RED)Application is not running or unhealthy$(RESET)"

wait-for-app: ## Wait for application to be ready
	@echo "$(GREEN)Waiting for application to be ready...$(RESET)"
	@timeout 60 bash -c 'until curl -f http://localhost:8080/actuator/health >/dev/null 2>&1; do sleep 2; done' && echo "$(GREEN)Application is ready!$(RESET)" || echo "$(RED)Timeout waiting for application$(RESET)"

dev: up wait-for-app ## Start services and wait for application to be ready
	@echo "$(GREEN)Development environment is ready!$(RESET)"

dev-logs: dev logs ## Start services and show logs

test: ## Run full test suite in Docker
	@echo "$(GREEN)Running tests in Docker...$(RESET)"
	docker compose -f docker-compose.yml -f docker-compose.test.yml run --rm app-test

info: ## Show project information
	@echo "$(GREEN)Greg Fullstack Application$(RESET)"
	@echo "  Spring Boot: 3.5.6"
	@echo "  Java: 25"
	@echo "  Gradle: 9.1.0"
	@echo "  PostgreSQL: 16"
	@echo "  Docker Compose: $(shell docker compose version --short 2>/dev/null || echo 'Not installed')"
	@echo "  Docker: $(shell docker version --format '{{.Server.Version}}' 2>/dev/null || echo 'Not installed')"
