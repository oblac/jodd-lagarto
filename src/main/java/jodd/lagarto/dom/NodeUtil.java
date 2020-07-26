package jodd.lagarto.dom;

public class NodeUtil {

	public static Node[] join(final Node[] array1, final Node[] array2) {
		final int length = array1.length + array2.length;
		final Node[] result = new Node[length];

		System.arraycopy(array1, 0, result, 0, array1.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

}
