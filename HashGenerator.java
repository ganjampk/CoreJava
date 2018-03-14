import java.security.MessageDigest;

public class HashGenerator {

	public byte[] generateSha256Hash(final byte[] message) {
		final String algorithm = "SHA-256";
		final String securityProvider = "SUN";

		byte[] hash = null;

		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithm, securityProvider);
			digest.reset();
			hash = digest.digest(message);
		} catch (final Exception e) {
			e.printStackTrace();
		}

		return hash;
	}

	public static void main(final String[] args) {

		final HashGenerator generator = new HashGenerator();
		final String inputString = "Fis@1234";
		System.out.println(generator.generateSha256Hash(inputString.getBytes()));
	}

}
