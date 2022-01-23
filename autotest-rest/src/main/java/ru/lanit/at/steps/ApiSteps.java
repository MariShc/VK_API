package ru.lanit.at.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.ru.И;
import io.qameta.allure.Allure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import ru.lanit.at.api.ApiRequest;
import ru.lanit.at.api.models.RequestModel;
import ru.lanit.at.api.testcontext.ContextHolder;
import ru.lanit.at.utils.CompareUtil;
import ru.lanit.at.utils.DataGenerator;
import ru.lanit.at.utils.Sleep;
import ru.lanit.at.utils.VariableUtil;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static ru.lanit.at.api.testcontext.ContextHolder.replaceVarsIfPresent;
import static ru.lanit.at.utils.JsonUtil.getFieldFromJson;

public class ApiSteps {

    private static final Logger LOG = LoggerFactory.getLogger(ApiSteps.class);
    private ApiRequest apiRequest;
    private final Map<String, String> queryParams = new HashMap<>();

    @И("создать запрос")
    public void createRequest(RequestModel requestModel) {
        apiRequest = new ApiRequest(requestModel);
        System.out.println("requestModel = " + requestModel);
    }

    @И("добавить header")
    public void addHeaders(DataTable dataTable) {
        Map<String, String> headers = new HashMap<>();
        dataTable.asLists().forEach(it -> headers.put(it.get(0), it.get(1)));
        apiRequest.setHeaders(headers);
    }

    @И("добавить query параметры")
    public void addQuery(DataTable dataTable) {
        Map<String, String> query = new HashMap<>();
        dataTable.asLists().forEach(it -> {
            if (it.get(1).startsWith("$"))
                query.put(it.get(0), replaceVarsIfPresent(it.get(1)));
            else
                query.put(it.get(0), it.get(1));
        });
        apiRequest.setQuery(query);
    }

    @И("добавить параметры для upload_url")
    public void addQueryUpload() {
        apiRequest.setQuery(queryParams);
    }

    @И("отправить запрос")
    public void send() {
        apiRequest.sendRequest();
    }

    @И("получение ответа и запись тела ответа в файл")
    public void writeResponseToFile() {
        try {
            File filePath = new File("src/test/resources/json/outputProfileInfo.json");
            FileWriter file = new FileWriter(filePath);
            file.write(apiRequest.getResponse().body().prettyPrint());
            file.flush();
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @И("статус код {int}")
    public void expectStatusCode(int code) {
        int actualStatusCode = apiRequest.getResponse().statusCode();
        Assert.assertEquals(actualStatusCode, code);
    }

    ObjectMapper mapper = new ObjectMapper();

    @И("извлечь данные")
    public void extractVariables(Map<String, String> vars) {
        String responseBody;
        if (apiRequest.getResponse().contentType().contains("html")) {
            responseBody = String.valueOf(apiRequest.getResponse().body().htmlPath().get().children());
//            responseBody = responseBody.replace("\\", "");
        } else {
            responseBody = apiRequest.getResponse().body().asPrettyString();
        }
        vars.forEach((k, jsonPath) -> {
            jsonPath = replaceVarsIfPresent(jsonPath);
            String extractedValue = VariableUtil.extractBrackets(getFieldFromJson(responseBody, jsonPath));
            ContextHolder.put(k, extractedValue);
            Allure.addAttachment(k, "application/json", extractedValue, ".txt");
            LOG.info("Извлечены данные: {}={}", k, extractedValue);
        });
        if (queryParams.isEmpty() && responseBody.contains("upload_url")) {
            String body = responseBody.split("\\?")[1];
            String[] params = body.split("&");

            for (String item : params) {
                queryParams.put(item.split("=")[0], item.split("=")[1]);
            }
        }
    }

    @И("сгенерировать переменные")
    public void generateVariables(Map<String, String> table) {
        table.forEach((k, v) -> {
            String value = DataGenerator.generateValueByMask(replaceVarsIfPresent(v));
            ContextHolder.put(k, value);
            Allure.addAttachment(k, "application/json", k + ": " + value, ".txt");
            LOG.info("Сгенерирована переменная: {}={}", k, value);
        });
    }

    @И("создать контекстные переменные")
    public void createContextVariables(Map<String, String> table) {
        table.forEach((k, v) -> {
            ContextHolder.put(k, v);
            LOG.info("Сохранена переменная: {}={}", k, v);
        });
    }

    @И("сравнить значения")
    public void compareVars(DataTable table) {
        table.asLists().forEach(it -> {
            String expect = replaceVarsIfPresent(it.get(0));
            String actual = replaceVarsIfPresent(it.get(2));
            boolean compareResult = CompareUtil.compare(expect, actual, it.get(1));
            Assert.assertTrue(compareResult, String.format("Ожидаемое: '%s'\nФактическое: '%s'\nОператор сравнения: '%s'\n", expect, actual, it.get(1)));
            Allure.addAttachment(expect, "application/json", expect + it.get(1) + actual, ".txt");
            LOG.info("Сравнение значений: {} {} {}", expect, it.get(1), actual);
        });
    }

    @И("подождать {int} сек")
    public void waitSeconds(int timeout) {
        Sleep.pauseSec(timeout);
    }
}
