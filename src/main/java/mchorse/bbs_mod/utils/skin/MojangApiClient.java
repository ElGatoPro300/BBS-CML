package mchorse.bbs_mod.utils.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;


public class MojangApiClient
{
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    private static final String PROFILE_ENDPOINT = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_ENDPOINT = "https://sessionserver.mojang.com/session/minecraft/profile/";

    /**
     * Resolves a player's skin URL from their username.
     */
    public String getSkinUrlFromName(String playerName) throws IOException
    {
        String uuid = fetchUuid(playerName);
        String texturesJson = fetchDecodedTextures(uuid);
        return extractSkinUrl(texturesJson);
    }

    private String fetchUuid(String playerName) throws IOException
    {
        String response = httpGet(PROFILE_ENDPOINT + playerName);
        if (response == null)
        {
            throw new IOException("Oyuncu için skin URL bulunamadı: " + playerName);
        }

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        return json.get("id").getAsString();
    }

    private String fetchDecodedTextures(String uuid) throws IOException
    {
        String response = httpGet(SESSION_ENDPOINT + uuid);
        if (response == null)
        {
            throw new IOException("Oyuncu için skin URL bulunamadı (profile): " + uuid);
        }

        JsonObject profileJson = JsonParser.parseString(response).getAsJsonObject();
        String encodedTextures = profileJson.getAsJsonArray("properties")
                .get(0)
                .getAsJsonObject()
                .get("value")
                .getAsString();

        return new String(Base64.getDecoder().decode(encodedTextures));
    }

    private String extractSkinUrl(String decodedTexturesJson)
    {
        JsonObject texturesJson = JsonParser.parseString(decodedTexturesJson).getAsJsonObject();
        return texturesJson.getAsJsonObject("textures")
                .getAsJsonObject("SKIN")
                .get("url")
                .getAsString();
    }


    private String httpGet(String urlString) throws IOException
    {
        HttpURLConnection connection = openConnection(urlString);

        if (connection.getResponseCode() != 200)
        {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
        {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null)
            {
                response.append(line);
            }

            return response.toString();
        }
    }

    private HttpURLConnection openConnection(String urlString) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        return connection;
    }
}