package ru.lanit.at.api;

import io.qameta.allure.Allure;
import io.restassured.builder.MultiPartSpecBuilder;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.specification.MultiPartSpecification;
import io.restassured.specification.RequestSpecification;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.core5.http.ContentType;
import ru.lanit.at.api.listeners.RestAssuredCustomLogger;
import ru.lanit.at.api.models.RequestModel;
import ru.lanit.at.api.properties.RestConfigurations;
import ru.lanit.at.api.testcontext.ContextHolder;
import ru.lanit.at.utils.FileUtil;
import ru.lanit.at.utils.JsonUtil;
import ru.lanit.at.utils.RegexUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static io.restassured.RestAssured.given;
import static ru.lanit.at.api.testcontext.ContextHolder.replaceVarsIfPresent;

public class ApiRequest {

    private final static RestConfigurations CONFIGURATIONS = ConfigFactory.create(RestConfigurations.class,
            System.getProperties(),
            System.getenv());


    private String baseUrl;
    private String path;
    private Method method;
    private String body;
    private String fullUrl;
    private Response response;
    private String vkAccessToken;
    private String vkVersion;

    private RequestSpecBuilder builder;

    public ApiRequest(RequestModel requestModel) {
        this.builder = new RequestSpecBuilder();

        this.baseUrl = CONFIGURATIONS.getBaseUrl();
        this.path = replaceVarsIfPresent(requestModel.getPath());
        this.method = Method.valueOf(requestModel.getMethod());
        this.body = requestModel.getBody();
        this.fullUrl = replaceVarsIfPresent(requestModel.getUrl());
        this.vkAccessToken = CONFIGURATIONS.getVkAccessToken();
        this.vkVersion = CONFIGURATIONS.getVkVersion();

        URI uri;

        if (!fullUrl.isEmpty()) {
            uri = URI.create(fullUrl.replace(" ", "+"));
        } else {
            uri = URI.create(baseUrl);
            builder.setBasePath(path);
        }

        this.builder.setBaseUri(uri);
        setBodyFromFile();
        builder.addQueryParam("access_token", vkAccessToken);
        builder.addQueryParam("v", vkVersion);
        addLoggingListener();
    }

    public Response getResponse() {
        return response;
    }

    /**
     * Сеттит заголовки
     */
    public void setHeaders(Map<String, String> headers) {
        headers.forEach((k, v) -> {
            builder.addHeader(k, v);
        });
    }

    /**
     * Сеттит query-параметры
     */
    public void setQuery(Map<String, String> query) {
        query.forEach((k, v) -> {
            builder.addQueryParam(k, v);
        });
    }

    /**
     * Отправляет сформированный запрос
     */
    public void sendRequest() {
        RequestSpecification requestSpecification = builder.build();

        Response response = given()
                .spec(requestSpecification)
                .request(method);

        attachRequestResponseToAllure(response, body);
        this.response = response;
    }

    /**
     * Сессит тело запроса из файла
     */
    private void setBodyFromFile() {
        if (body != null && RegexUtil.getMatch(body, ".*\\.json")) {
            body = replaceVarsIfPresent(FileUtil.readBodyFromJsonDir(body));
            builder.setBody(body);
        }
        if (body != null && RegexUtil.getMatch(body, ".*\\.jpg")) {
            String filesPackage = "src/test/resources/files/";
            File file = new File(filesPackage + body);
            builder.addMultiPart(new MultiPartSpecBuilder(file).
                    fileName(body).controlName("photo").
                    mimeType("image/jpg").build());
        }
    }

    /**
     * Аттачит тело запроса и тело ответа в шаг отправки запроса
     */
    private void attachRequestResponseToAllure(Response response, String requestBody) {
        if (requestBody != null) {
            Allure.addAttachment(
                    "Request",
                    "application/json",
                    IOUtils.toInputStream(requestBody, StandardCharsets.UTF_8),
                    ".txt");
        }
        String responseBody = JsonUtil.jsonToUtf(response.body().asPrettyString());
        Allure.addAttachment("Response", "application/json", responseBody, ".txt");
    }

    /**
     * Добавляет логгер, печатающий в консоль данные запросов и ответов
     */
    private void addLoggingListener() {
        builder.addFilter(new RestAssuredCustomLogger());
    }
}

