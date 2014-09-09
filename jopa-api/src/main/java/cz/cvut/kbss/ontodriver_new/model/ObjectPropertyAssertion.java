package cz.cvut.kbss.ontodriver_new.model;

import java.net.URI;

final class ObjectPropertyAssertion extends PropertyAssertion {

	private static final long serialVersionUID = 9210709887861831464L;

	ObjectPropertyAssertion(URI assertionIdentifier, boolean isInferred) {
		super(assertionIdentifier, isInferred);

	}

	@Override
	public AssertionType getType() {
		return AssertionType.OBJECT_PROPERTY;
	}

	@Override
	public int hashCode() {
		int prime = 31;
		return prime * super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}
}