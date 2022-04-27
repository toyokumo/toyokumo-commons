.PHONY: docker-stop
docker-stop:
	cd dev-resources && docker-compose stop

.PHONY: docker-up
docker-up: docker-stop
	cd dev-resources && docker-compose up -d

.PHONY: docker-down
docker-down: docker-stop
	cd dev-resources && docker-compose down

.PHONY: docker-ps
docker-ps:
	cd dev-resources && docker-compose ps

.PHONY: test
test:
	clojure -M:dev:test

.PHONY: install
install: clean
	clojure -T:build install

.PHONY: outdated
outdated:
	clojure -M:outdated

.PHONY: clean
clean:
	\rm -rf target .cpcache
