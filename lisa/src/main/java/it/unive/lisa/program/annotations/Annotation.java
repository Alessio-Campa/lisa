package it.unive.lisa.program.annotations;

import java.util.List;

/**
 * A single annotation.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Annotation {

	private final String annotationName;

	private final List<AnnotationMember> annotationMembers;

	/**
	 * Builds an annotation from its name and its members.
	 * 
	 * @param annotationName    the name of the annotation
	 * @param annotationMembers the annotation members
	 */
	public Annotation(String annotationName, List<AnnotationMember> annotationMembers) {
		this.annotationMembers = annotationMembers;
		this.annotationName = annotationName;
	}

	/**
	 * Yields the annotation members of this annotation.
	 * 
	 * @return the annotation members of this annotation
	 */
	public List<AnnotationMember> getAnnotationMembers() {
		return annotationMembers;
	}

	/**
	 * Yields the annotation name of this annotation.
	 * 
	 * @return the annotaiton name of this annotaiton
	 */
	public String getAnnotationName() {
		return annotationName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotationMembers == null) ? 0 : annotationMembers.hashCode());
		result = prime * result + ((annotationName == null) ? 0 : annotationName.hashCode());
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
		Annotation other = (Annotation) obj;
		if (annotationMembers == null) {
			if (other.annotationMembers != null)
				return false;
		} else if (!annotationMembers.equals(other.annotationMembers))
			return false;
		if (annotationName == null) {
			if (other.annotationName != null)
				return false;
		} else if (!annotationName.equals(other.annotationName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		if (annotationMembers == null)
			return annotationName;

		return annotationName + annotationMembers.toString();
	}
}
