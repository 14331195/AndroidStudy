
import java.io.*;

public class zip {
	public zip() {

	}


	public static void main(String[] args) {
		File file = new File("C://apps//AndroidStudy", "../aaaaaaaaaaa.txt");
		try {
			file.createNewFile();
		} catch (IOException e) {

		}
		System.out.println("ss");
	}
}