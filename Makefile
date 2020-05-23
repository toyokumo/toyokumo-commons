docker-stop:
	cd dev-resources && docker-compose stop

docker-up: docker-stop
	cd dev-resources && docker-compose up -d

docker-down: docker-stop
	cd dev-resources && docker-compose down

docker-ps:
	cd dev-resources && docker-compose ps
