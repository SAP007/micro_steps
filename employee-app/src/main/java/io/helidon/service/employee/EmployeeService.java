package io.helidon.service.employee;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;

import io.helidon.common.http.Http;
import io.helidon.config.Config;
import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

public  class EmployeeService implements Service {

    private final EmployeeRepository employees;

    private static final Logger LOGGER = Logger.getLogger(EmployeeService.class.getName());

    EmployeeService(Config config) {
        employees = EmployeeRepository.create(config.get("app.drivertype").asString().orElse("Array"), config);
    }

    public void update(Routing.Rules rules) {
        rules.get("/", this::getAll)
            .get("/lastname/{name}", this::getByLastName)
            .get("/department/{name}", this::getByDepartment)
            .get("/title/{name}", this::getByTitle)
            .post("/", this::save)
            .get("/{id}", this::getEmployeeById)
            .put("/{id}", this::update)
            .delete("/{id}", this::delete);
    }

    //Method we have created not in tutorial
    private void getByTitle(final ServerRequest request, final ServerResponse response) {
        LOGGER.fine("getByTitle");
        // Invalid query strings handled in isValidQueryStr. Keeping DRY
        if (isValidQueryStr(response, request.path().param("name"))) {
            response.status(200).send(this.employees.getByTitle(request.path().param("name")));
        }
    }

    //Method we have created not in tutorial
    private void getByDepartment(final ServerRequest request, final ServerResponse response) {
        LOGGER.fine("getByDepartment");
        // Invalid query strings handled in isValidQueryStr. Keeping DRY
        if (isValidQueryStr(response, request.path().param("name"))) {
            response.status(200).send(this.employees.getByDepartment(request.path().param("name")));
        }
    }

    private void getAll(final ServerRequest request, final ServerResponse response) {
        LOGGER.fine("getAll");
        List<Employee> allEmployees = this.employees.getAll();
        response.send(allEmployees);
    }

    private void getByLastName(final ServerRequest request, final ServerResponse response) {
        LOGGER.fine("getByLastName");
        // Invalid query strings handled in isValidQueryStr. Keeping DRY
        if (isValidQueryStr(response, request.path().param("name"))) {
            response.status(200).send(this.employees.getByLastName(request.path().param("name")));
        }
    }

    private void getEmployeeById(ServerRequest request, ServerResponse response) {
        LOGGER.fine("getEmployeeById");
        // If invalid, response handled in isValidId. Keeping DRY
        if (isValidId(response, request.path().param("id"))) {
            Employee employee = this.employees.getById(request.path().param("id"));
            response.status(200).send(employee);
        }
    }


    private boolean isValidQueryStr(ServerResponse response, String nameStr) {
        Map<String, String> errorMessage = new HashMap<>();
        if (nameStr == null || nameStr.isEmpty() || nameStr.length() > 100) {
            errorMessage.put("errorMessage", "Invalid query string");
            response.status(400).send(errorMessage);
            return false;
        } else {
            return true;
        }
    }

    private boolean isValidId(ServerResponse response, String idStr) {
        Map<String, String> errorMessage = new HashMap<>();
        if (idStr == null || idStr.isEmpty()) {
            errorMessage.put("errorMessage", "Invalid query string");
            response.status(400).send(errorMessage);
            return false;
        } else if (this.employees.isIdFound(idStr)) {
            return true;
        } else {
            errorMessage.put("errorMessage", "ID " + idStr + " not found");
            response.status(404).send(errorMessage);
            return false;
        }
    }

    private void save(ServerRequest request, ServerResponse response) {
        LOGGER.fine("save");
        request.content().as(Employee.class)
            .thenApply(e -> Employee.of(null, e.getFirstName(), e.getLastName(),
                e.getEmail(), e.getPhone(), e.getBirthDate(), e.getTitle(), e.getDepartment()))
            .thenApply(this.employees::save).thenCompose(p -> response.status(201).send());
    }

    private void update(ServerRequest request, ServerResponse response) {
        LOGGER.fine("update");
        if (isValidId(response, request.path().param("id"))) {
            request.content().as(Employee.class).thenApply(e -> {
                return this.employees.update(Employee.of(e.getId(),
                    e.getFirstName(), e.getLastName(), e.getEmail(),
                    e.getPhone(), e.getBirthDate(), e.getTitle(), e.getDepartment()),
                    request.path().param("id"));
            }).thenCompose(p -> response.status(204).send());
        }
    }

    private void delete(final ServerRequest request, final ServerResponse response) {
        LOGGER.fine("delete");
        if (isValidId(response, request.path().param("id"))) {
            this.employees.deleteById(request.path().param("id"));
            response.status(204).send();
        }
    }



}

