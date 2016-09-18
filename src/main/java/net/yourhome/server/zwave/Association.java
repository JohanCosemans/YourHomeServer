/*-
 * Copyright (c) 2016 Coteq, Johan Cosemans
 * All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY COTEQ AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.yourhome.server.zwave;

public class Association {
	private short fromNode;
	private short toNode;
	private int associationClass;

	public Association(short fromNode, short toNode, int associationClass) {
		this.fromNode = fromNode;
		this.toNode = toNode;
		this.associationClass = associationClass;
	}

	/**
	 * @return the fromNode
	 */
	public short getFromNode() {
		return this.fromNode;
	}

	/**
	 * @param fromNode
	 *            the fromNode to set
	 */
	public void setFromNode(short fromNode) {
		this.fromNode = fromNode;
	}

	/**
	 * @return the toNode
	 */
	public short getToNode() {
		return this.toNode;
	}

	/**
	 * @param toNode
	 *            the toNode to set
	 */
	public void setToNode(short toNode) {
		this.toNode = toNode;
	}

	/**
	 * @return the associationClass
	 */
	public int getAssociationClass() {
		return this.associationClass;
	}

	/**
	 * @param associationClass
	 *            the associationClass to set
	 */
	public void setAssociationClass(int associationClass) {
		this.associationClass = associationClass;
	}

}
