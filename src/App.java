import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    public String logOutput;

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
        this.logOutput = "logs/log-" + UUID.randomUUID().toString() + ".txt";
    }

    public String prompt(String message) throws IOException, InterruptedException {
        messages.add(new Message("user", message));

        PromptRequest promptRequest = new PromptRequest(model, messages, true);
        Gson gson = new Gson();

        String payload = gson.toJson(promptRequest);
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:11434/api/chat"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8));

        String text = "";
        String line;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                String content = new Gson().fromJson(line, OllamaResponse.class).message.getContent();
                System.out.print(content);
                text += content;
            }
        }

        log(text);

        return text;
    }

    public String getMessageHistory() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(messages);
    }

    public void log(String data) throws IOException {
        FileWriter writer = new FileWriter(logOutput, true);
        writer.append(data);
        writer.append("\n-------------------------------------------------------------------\n\n");
        writer.close();
        System.out.println(data);
        System.out.println("-------------------------------------------------------------------\n");
    }
}

class Item {

    public enum Version { SATURATION, HEALTH, DAMAGE }   

    public String name;
    public Version type;
    public int amount;
    public int quantity;

    Item(String name, Version type, int amount, int quantity) {
        this.name = name;
        this.type = type;
        this.amount = amount;
        this.quantity = quantity;
    }
}

class Person {
    private String name;
    private String personality;

    private int maxHealth;
    private int health;
    private int maxSaturation;
    private int saturation;

    HashMap<String, Item> items;

    private Ollama model;

    Person(String name, String personality, int health, int saturation, HashMap<String, Item> items, String model) {
        this.name = name;
        this.personality = personality;

        this.maxHealth = health;
        this.health = health;

        this.maxSaturation = saturation;
        this.saturation = saturation;

        this.items = items;
        if (items == null)
            this.items = new HashMap<String, Item>();

        this.model = new Ollama(
            model,
            "Pretend you are a game character with this personality:" + this.personality,
            null
        );
    }

    public Ollama getModel() {
        return model;
    }

    public void setModel(Ollama model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public void takeDamage(int damage) {
        this.health -= damage;
        this.health = Math.clamp(health, 0, this.maxHealth);
    }

    public void addHealth(int health) {
        takeDamage(-health);
    }

    public int getSaturation() {
        return saturation;
    }

    public void takeSaturation(int saturation) {
        this.saturation -= saturation;
        this.saturation = Math.clamp(this.saturation, 0, this.maxSaturation);
    }

    public void addSaturation(int saturation) {
        takeSaturation(-saturation);
    }

    public void grabItem(int index, ArrayList<Item> itemPool) {
        Item item = itemPool.get(index);
        if (this.items.containsKey(item.name))
            item.quantity += this.items.get(item.name).quantity;
        this.items.put(name, item);
    }

    public String talk(Person person, String prompt) throws InterruptedException, IOException {
        String message = person.model.prompt(prompt);
        return message;
    }

    public void useItem(Person entity, String name, int quantity) {
        Item item = this.items.get(name);
        item.quantity -= quantity;
        switch (item.type) {
            case SATURATION:
                entity.addSaturation(item.amount * quantity);
                break;
            case HEALTH:
                entity.addHealth(item.amount * quantity);
                break;
            case DAMAGE:
                entity.takeDamage(item.amount * quantity);
                break;
        }
        this.items.put(name, item);
    }

    public String metaData() {
        String name = "Name: " + this.name;
        String personality = "Personality: " + this.personality;
        String health = "Current Health: " + this.health + "\tMax Health: " + this.maxHealth;
        String saturation = "Current Saturation: " + this.saturation + "\tMax Saturation: " + this.maxSaturation;

        String items = "Items:\n";
        for (Map.Entry<String, Item> entry : this.items.entrySet()) {
            Item item = entry.getValue();
            items += "\tName: " + item.name + "\tType: " + item.type + "\tMagnitude: " + item.amount + "\tQuantity: " + item.quantity;
        }

        return String.join("\n", new String[] {"These are your stats", name, personality, health, saturation, items});
    }
}

public class App {
    public static void main(String[] args) throws IOException, InterruptedException {
        Person person1 = new Person(
            "Jessica",
            "She is the most awful person on the entire planet", 
            100, 
            100, 
            new HashMap<String, Item>() {{
                put("Awesome Sauce", new Item(
                    "Awesome Sauce", 
                    Item.Version.HEALTH, 
                    30, 
                    800
                ));
            }}, 
            "mistral"
        );

        person1.getModel().prompt(
            person1.metaData() + "Would you give someone with only half your health some health packs; they have no other mechanism to get help"
        );
    }
}
