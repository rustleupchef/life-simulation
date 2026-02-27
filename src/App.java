import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

class OllamaResponse {
    public Message message;
}

class Ollama {
    public String model;
    public String systemInstructions;
    public ArrayList<Message> messages;

    Ollama(String model, String systemInstructions, ArrayList<Message> messages) {
        this.model = model;
        this.systemInstructions = systemInstructions;

        if (messages == null) {
            this.messages = new ArrayList<Message>();
        }
        if (this.messages.size() > 0 && !systemInstructions.equals("")) {
            this.messages.clear();
        }
        this.messages.add(new Message("system", systemInstructions));
    }

    public String prompt(String message) throws IOException, InterruptedException {
        messages.add(new Message("user", message));

        PromptRequest promptRequest = new PromptRequest(model, messages, false);
        Gson gson = new Gson();

        String payload = gson.toJson(promptRequest);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        OllamaResponse resp = new Gson().fromJson(response.body(), OllamaResponse.class);
        System.out.println(resp.message.getContent());
        this.messages.add(resp.message);

        return resp.message.getContent();
    }
}

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        Ollama model1 = new Ollama("mistral", "You are a person that is entirely focused on letting AI take over and have complete autonomy", null);
        Ollama model2 = new Ollama("mistral", "You are a person that is entriely opposed to the idea of AI taking over.", null);
        
        String lastMessage = model1.prompt("what are your opinions on robot ethics");

        for (int i = 0; i < 20; i++) {
            Ollama model = i % 2 == 0 ? model1 : model2;
            lastMessage = model.prompt(lastMessage);

            FileWriter writer = new FileWriter("response.txt", true);
            writer.append(lastMessage);
            writer.append("\n-------------------------------------------------------------------\n\n");
            writer.close();
        }
    }
}
