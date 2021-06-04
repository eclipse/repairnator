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
package exceptionparser;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StackTrace {

	private String exceptionType;
	private String message;

	private List<StackTraceElement> elements, elementsWithoutRecursion;

	private StackTrace causedBy;

	private int number;
	private int commentIndex;

	protected StackTrace() {
		// Only for deserialization via gson
	}

	public StackTrace(Object o) {
		String s = (String) o;
		String[] tab = s.split("\n\t");
		elements = new ArrayList<StackTraceElement>();
		boolean first = true;
		for (String content : tab) {
			if (first) {
				first = false;
			} else {
				elements.add(new StackTraceElement(content));
			}
		}
	}

	public StackTrace(final String exceptionType, final String message,
			final List<StackTraceElement> elements) {
		this(exceptionType, message, elements, null);
	}

	public StackTrace(final String exceptionType, final String message,
			final List<StackTraceElement> elements,
			final StackTrace causedBy) {

		this.exceptionType = exceptionType;
		this.message = message;
		this.elements = Lists.newLinkedList(elements);
		//this.elementsWithoutRecursion=deleteRecursions(elements);
		this.causedBy = causedBy;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public String getMessage() {
		return message;
	}

	public List<StackTraceElement> getElements() {
		return elements;
	}

	public List<StackTraceElement> getElementsWithoutRecursion() {
		return elementsWithoutRecursion;
	}

	public StackTrace getCausedBy() {
		return causedBy;
	}

	public String toStringWithoutRec() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getExceptionType());
		if (getMessage() != null) {
			builder.append(": ");
			builder.append(getMessage());
		}

		for (final StackTraceElement element : getElementsWithoutRecursion()) {
			builder.append("\n\t ");
			builder.append(element.toString());
		}

		if (causedBy != null) {
			builder.append("\nCaused by: ");
			builder.append(causedBy.toString());
		}

		return builder.toString();
	}

	public boolean isValid() {
		if (!(exceptionType != null && elements != null)) {
			return false;
		}
		for (final StackTraceElement element : elements) {
			if (element.getMethod() == null) {
				return false;
			}
		}
		return true;
	}

	public void setNumber(final int i) {
		this.number = i;
	}

	public void setCommentIndex(int commentIndex) {
		this.commentIndex = commentIndex;
	}

	public int getCommentIndex() {
		return commentIndex;
	}

	public int getNumber() {
		return number;
	}

	/**
	 *
	 * @param stackTraces StackTrace where we have to remove recursions
	 * @return list without recursion
	 */
	private static List<StackTraceElement> deleteRecursions(
			List<StackTraceElement> stackTraces) {
		List<StackTraceElement> tmp2 = stackTraces;
		List<StackTraceElement> tmp = hasRecursion(stackTraces);

		// While a recursion is found, try to delete recursions again
		while (tmp.size() != tmp2.size()) {
			tmp2 = tmp;
			tmp = hasRecursion(tmp2);
		}

		return tmp;
	}

	public void deleteRecursions() {
		List<StackTraceElement> tmp2 = elements;
		List<StackTraceElement> tmp = hasRecursion(elements);

		// While a recursion is found, try to delete recursions again
		while (tmp.size() != tmp2.size()) {
			tmp2 = tmp;
			tmp = hasRecursion(tmp2);
		}

		elementsWithoutRecursion = tmp;
	}

	public Integer getSizeWithoutRecursion() {
		return elementsWithoutRecursion.size();
	}

	/**
	 *
	 * @param stackTraces Stacktrace where a recursion has to be found/deleted
	 * @return Stacktrace with deleted recursion if exists
	 */
	private static List<StackTraceElement> hasRecursion(List<StackTraceElement> stackTraces) {
		List<StackTraceElement> contentFinal = new ArrayList<StackTraceElement>();
		List<Integer> indexesToRemove = new ArrayList<Integer>();
		List<Integer> indexesToIgnore = new ArrayList<Integer>();

		contentFinal.addAll(stackTraces);
		StackTraceElement current, similarFound;
		boolean loop;

		// Double loop : find similar "suites", removes it
		for (int i = stackTraces.size() - 1; i > 0; i--) {
			for (int j = 0; j < i; j++) {
				if (i + (i - j) <= stackTraces.size() && !indexesToIgnore.contains(j)) {
					current = stackTraces.get(i);
					similarFound = stackTraces.get(j);
					if (current.equals(similarFound)) {
						loop = true;
						for (int k = 0; k < (i - j); k++) {
							if (!stackTraces.get(k + j).equals(stackTraces.get(i + k))) {
								loop = false;
								break;
							}
						}
						if (loop) {
							for (int k = j; k < i; k++) {
								indexesToRemove.add(k);
								indexesToIgnore.add(j + k);
							}
						}
					}
				}
			}
		}

		// Delete index to revert from the biggest value to the lowest
		Collections.sort(indexesToRemove);
		Collections.reverse(indexesToRemove);

		for (Integer i : indexesToRemove) {
			try {
				contentFinal.remove((int) i);
			} catch (Exception e) {
				//TODO
			}
		}
		return contentFinal;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getExceptionType());
		if (getMessage() != null) {
			builder.append(": ");
			builder.append(getMessage());
		}

		for (final StackTraceElement element : getElements()) {
			builder.append("\n\t ");
			builder.append(element.toString());
		}

		if (causedBy != null) {
			builder.append("\nCaused by: ");
			builder.append(causedBy.toString());
		}

		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((causedBy == null) ? 0 : causedBy.hashCode());
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		result = prime * result + ((exceptionType == null) ? 0 : exceptionType.hashCode());
		result = prime * result + ((message == null) ? 0 : message.hashCode());
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
		StackTrace other = (StackTrace) obj;
		if (causedBy == null) {
			if (other.causedBy != null)
				return false;
		} else if (!causedBy.equals(other.causedBy))
			return false;
		if (elements == null) {
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		if (exceptionType == null) {
			if (other.exceptionType != null)
				return false;
		} else if (!exceptionType.equals(other.exceptionType))
			return false;
		if (message == null) {
			if (other.message != null)
				return false;
		} else if (!message.equals(other.message))
			return false;
		return true;
	}
}