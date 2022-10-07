import xyz.kumaraswamy.sketch.Sketch;
import xyz.kumaraswamy.sketch.nativs.Print;
import xyz.kumaraswamy.sketch.nativs.Printf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;

public class Main {

    private static final File directory = new File(System.getProperty("user.dir"));
    private static final File slime = new File(directory, "hello.sketch");
    private static final File slimeOut = new File(directory, "sketchout");

    public static void main(String[] args) throws IOException {
        /**
         * println(123)
         */
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Print.setOutputStream(stream);
        Printf.setOutputStream(stream);

        Sketch sketch = new Sketch();
        String lines = new String(Files.readAllBytes(Main.slime.toPath()));

        long start = System.nanoTime();
        sketch.execute(lines);
        long end   = System.nanoTime();

        Duration d = Duration.ofNanos( end - start ) ;

        System.out.println( d.getNano() + "ns" ) ;
        byte[] bytes = stream.toByteArray();
        // no need to close the stream
        Files.write(slimeOut.toPath(), bytes);

        System.out.println();
    }
}