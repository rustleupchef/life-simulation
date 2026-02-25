import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;

import com.google.gson.Gson;

class Message {
    private String role;
    private String content;

    Message(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

class PromptRequest {
    private String model;
    private ArrayList<Message> messages;
    private boolean stream;

    PromptRequest(String model, ArrayList<Message> messages, boolean stream) {
        this.model = model;
        this.messages = messages;
        this.stream = stream;
    }
}

class Ollama {
    public String model;
    public String systemInstructions;
    public int maxTokens;
    public ArrayList<Message> messages;

    Ollama(String model, String systemInstructions, int maxTokens, ArrayList<Message> messages) {
        this.model = model;
        this.systemInstructions = systemInstructions;
        this.maxTokens = maxTokens;

        if (messages.size() > 0 && !systemInstructions.equals("")) {
            messages.clear();
            messages.add(new Message("system", systemInstructions));
        }
    }

    public void prompt(String message) {
        messages.add(new Message("user", message));

        PromptRequest promptRequest = new PromptRequest(model, messages, false);
        Gson gson = new Gson();

        String payload = gson.toJson(promptRequest);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        while (true) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                System.out.println(response.body());

                messages.add(new Message("assistant", response.body()));
            } catch (Exception e) {
                continue;
            }
        }
    }
}

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("Hello, World!");
    }
}
