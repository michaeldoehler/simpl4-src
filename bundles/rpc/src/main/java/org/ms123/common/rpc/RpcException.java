/**
 * This file is part of SIMPL4(http://simpl4.org).
 *
 * 	Copyright [2014] [Manfred Sattler] <manfred@ms123.org>
 *
 * SIMPL4 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SIMPL4 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SIMPL4.  If not, see <http://www.gnu.org/licenses/>.
 */
/*
 Java JSON RPC
 RPC Java POJO by Novlog
 http://www.novlog.com

 This library is dual-licensed under the GNU Lesser General Public License (LGPL) and the Eclipse Public License (EPL).
 Check http://qooxdoo.org/license

 This library is also licensed under the Apache license.
 Check http://www.apache.org/licenses/LICENSE-2.0

 Contribution:
 This contribution is provided by Novlog company.
 http://www.novlog.com
 */
package org.ms123.common.rpc;

public class RpcException extends RuntimeException {

	protected Integer origin = null;

	protected Integer errorCode = null;

	protected Throwable cause;

	protected String message;

	public RpcException(Integer origin) {
		this(origin, null);
	}

	public RpcException(Integer origin, Integer errorCode) {
		super();
		this.origin = origin;
		this.errorCode = errorCode;
	}

	public RpcException(Integer origin, Integer errorCode, String message) {
		super(message);
		this.message = message;
		this.origin = origin;
		this.errorCode = errorCode;
	}

	public RpcException(Integer origin, Integer errorCode, String message, Throwable t) {
		super(message, t);
		this.message = message;
		this.cause = t;
		this.origin = origin;
		this.errorCode = errorCode;
	}

	public String getMessage() {
		return message;
	}

	public Throwable getCause() {
		return cause;
	}

	public Integer getOrigin() {
		return origin;
	}

	public Integer getErrorCode() {
		return errorCode;
	}
}
