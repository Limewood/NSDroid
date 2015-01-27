/*
 * Copyright (c) 2013 Joakim Lindskog
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.limewoodMedia.nsapi.holders;

/**
 * A data object for a nation's budget
 * All values are in percentages
 * @author Joakim Lindskog
 *
 */
public class Budget {
	public int environment;
	public int socialEquality;
	public int education;
	public int lawAndOrder;
	public int administration;
	public int welfare;
	public int spirituality;
	public int defence;
	public int publicTransport;
	public int healthCare;
	public int commerce;
	
	@Override
	public String toString() {
		return "\n\t\tadministration:"+administration+"%"
				+"\n\t\tcommerce:"+commerce+"%"
				+"\n\t\tdefence:"+defence+"%"
				+"\n\t\teducation:"+education+"%"
				+"\n\t\tenvironment:"+environment+"%"
				+"\n\t\thealthCare:"+healthCare+"%"
				+"\n\t\tlawAndOrder:"+lawAndOrder+"%"
				+"\n\t\tpublicTransport:"+publicTransport+"%"
				+"\n\t\tsocialEquality:"+socialEquality+"%"
				+"\n\t\tspirituality:"+spirituality+"%"
				+"\n\t\twelfare:"+welfare+"%";
	}
}