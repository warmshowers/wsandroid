package fi.bitrite.android.ws.host.impl;

import org.junit.rules.ExternalResource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ResourceFile extends ExternalResource {
    private String res;
    private File file = null;
    private InputStream stream;

    public ResourceFile(String res) {
        this.res = res;
    }

    public File getFile() throws IOException {
        if (file == null) {
            createFile();
        }
        return file;
    }

    public InputStream getInputStream() {
        return stream;
    }

    public InputStream createInputStream() {
        return getClass().getClassLoader().getResourceAsStream(res);
    }

    public String getContent() throws IOException {
        return getContent("utf-8");
    }

    public String getContent(String charSet) throws IOException {
        char[] tmp = new char[4096];
        StringBuilder b = new StringBuilder();
        try (InputStreamReader reader = new InputStreamReader(createInputStream(),
                Charset.forName(charSet))) {
            while (true) {
                int len = reader.read(tmp);
                if (len < 0) {
                    break;
                }
                b.append(tmp, 0, len);
            }
            reader.close();
        }
        return b.toString();
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        stream = getClass().getClassLoader().getResourceAsStream(res);
    }

    @Override
    protected void after() {
        try {
            if (stream != null) {
                stream.close();
            }
        } catch (IOException e) {
            // ignore
        }
        if (file != null) {
            file.delete();
        }
        super.after();
    }

    private void createFile() throws IOException {
        file = new File(".", res);
        try (InputStream stream = getClass().getResourceAsStream(res)) {
            file.createNewFile();
            FileOutputStream ostream = null;
            try {
                ostream = new FileOutputStream(file);
                byte[] buffer = new byte[4096];
                while (true) {
                    int len = stream.read(buffer);
                    if (len < 0) {
                        break;
                    }
                    ostream.write(buffer, 0, len);
                }
            } finally {
                if (ostream != null) {
                    ostream.close();
                }
            }
        }
    }

}
