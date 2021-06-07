package fi.hel.verkkokauppa.shared.model.impl;

import fi.hel.verkkokauppa.shared.model.Identifiable;

public class BaseIdentifiableDto implements Identifiable {

	private String id;

	protected BaseIdentifiableDto() {
	}

	protected BaseIdentifiableDto(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}

	@Override
	public final int hashCode() {
		return (id != null ? id.hashCode() : System.identityHashCode(this));
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (!this.getClass().isAssignableFrom(o.getClass())) {
			return false;
		}

		final BaseIdentifiableDto that = (BaseIdentifiableDto) o;
		if (id == null || that.id == null) {
			return false;
		}

		return id.equals(that.id);
	}


	@Override
	public String toString() {
		return ("{id=" + id + "}");
	}

}
