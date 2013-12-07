
public class RangeTuple {
	int lineStart;
	int lineEnd;
	
	public RangeTuple(int lineStart, int lineEnd) {
		this.lineStart = lineStart;
		this.lineEnd = lineEnd;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + lineEnd;
		result = prime * result + lineStart;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RangeTuple other = (RangeTuple) obj;
		if (lineEnd != other.lineEnd)
			return false;
		if (lineStart != other.lineStart)
			return false;
		return true;
	}
}
