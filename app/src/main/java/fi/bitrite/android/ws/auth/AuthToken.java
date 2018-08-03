package fi.bitrite.android.ws.auth;


public class AuthToken {
    public final String name;
    public final String id;

    public AuthToken(String name, String id) {
        this.name = name;
        this.id = id;
    }

    @Override
    public String toString() {
        return name + "=" + id;
    }

    public static AuthToken fromString(String authTokenStr) {
        String[] authTokenParts = authTokenStr.split("=", 2);

        String name = authTokenParts[0];
        String id = (authTokenParts.length == 2) ? authTokenParts[1] : "";

        return new AuthToken(name, id);
    }

    @Override
    public boolean equals(Object other) {
        return other != null
               && other instanceof AuthToken
               && toString().equals(other.toString());
    }
}
