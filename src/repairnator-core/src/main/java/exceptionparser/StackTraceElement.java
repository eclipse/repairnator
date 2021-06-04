package exceptionparser;

/**
 * Copyright (c) 2010 Darmstadt University of Technology.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Johannes Lerch - initial API and implementation.
 */

public class StackTraceElement {

	private String method;
	private String source;

	protected StackTraceElement() {
		// only for deserialization
	}

	public StackTraceElement(final String method) {
		this(method, null);
	}

	/**
	 * Typical format of a StackTraceElement is '<i>at method(source)</i>',
	 * where method is a full qualified method as in 'a.b.C.m'. In the default
	 * implementation source may be one of 'filename.java:linenumber', 'Unknown
	 * Source' or 'Native Method'.
	 * <p>
	 * Please note that these are only examples for source, it is possible that
	 * implementations override these definitions to provide completely
	 * different informations.
	 * </p>
	 */
	public StackTraceElement(final String method, final String source) {
		this.method = method;
		this.source = source;
	}

	public String getMethod() {
		return method;
	}

	public String getSource() {
		return source;
	}

	@Override
	public String toString() {
		return String.format("at %s(%s)", method, getSource() == null ? "" : getSource());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((method == null) ? 0 : method.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
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
		StackTraceElement other = (StackTraceElement) obj;
		if (method == null) {
			if (other.method != null)
				return false;
		} else if (!method.equals(other.method))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}
}