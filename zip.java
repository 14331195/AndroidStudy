
import java.io.*;

// public class father {
// 	public String s = "father";
// }

public class zip extends father{
	public zip() {
		s = "zip";
	}

	@Override
	public void destroy() {
		super.destroy();
		System.out.println(s);
	}

	public static void main(String[] args) {
		// File file = new File("C://apps//AndroidStudy", "../aaaaaaaaaaa.txt");
		// try {
		// 	file.createNewFile();
		// } catch (IOException e) {

		// }
		zip z = new zip();
		z.destroy();
	}
}