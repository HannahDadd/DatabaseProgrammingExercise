package sjdb;

import java.util.Iterator;

public class Estimator implements PlanVisitor {


	public Estimator() {
		// empty constructor
	}
	
	/**
	 * Depth first traversal of query plan
	 */

	/* 
	 * Create output relation on Scan operator
	 *
	 * Example implementation of visit method for Scan operators.
	 */
	public void visit(Scan op) {
		Relation input = op.getRelation();
		Relation output = new Relation(input.getTupleCount());
		
		Iterator<Attribute> iter = input.getAttributes().iterator();
		while (iter.hasNext()) {
			output.addAttribute(new Attribute(iter.next()));
		}
		
		op.setOutput(output);
	}
	
	/**
	 * Project certain attributes from operator
	 */
	public void visit(Project op) {
		// Size of output relation is number of tuples in relation
		Relation input = op.getInput().getOutput();
		Relation output = new Relation(input.getTupleCount());
		
		for(Attribute a : op.getAttributes()) {
			// See if that attribute is in relation, if it is add it to output relation
			for(Attribute aInInput : input.getAttributes()) {
				if(a.getName() == aInInput.getName()) {
					output.addAttribute(aInInput);
				}
			}
		}
		op.setOutput(output);
	}
	
	/**
	 * Selects certain tuples depending on a predicate
	 */
	public void visit(Select op) {
		
		// See if predicate is attr=value
		if(op.getPredicate().equalsValue()) {
			// Create an output relation with 1 attribute
			Relation output = new Relation(1);
			
			// Set the output relation to a relation containing only the attributes that has that vlaue (name?)
			for(Attribute a : op.getInput().getOutput().getAttributes()) {
				if(a.getName() == op.getPredicate().getRightValue()) {
					output.addAttribute(a);
					break;
				}
			}
			op.setOutput(output);
		} else {
			// attr = attr
			
			// Find attributes in relation with same name
			Attribute firstAttr = new Attribute("NO ATTR");
			Attribute secondAttr = new Attribute("NO ATTR");
			// getAttribute won't work as it uses the object ref. and they would have different object refs as 2 seperate objects
			for(Attribute a : op.getInput().getOutput().getAttributes()) {
				if(op.getPredicate().getLeftAttribute().getName() == a.getName()) {
					firstAttr = a;
				}
				if(op.getPredicate().getRightAttribute().getName() == a.getName()) {
					secondAttr = a;
				}
			}
			// If an attribute hasn't been found set output to blank relation otherwise check value count
			Relation output = new Relation(Math.min(firstAttr.getValueCount(), secondAttr.getValueCount()));
			if(firstAttr.getValueCount() == secondAttr.getValueCount() && 
				(firstAttr.getName() == "NO ATTR" || secondAttr.getName() == "NO ATTR")) {
				output.addAttribute(secondAttr);
				output.addAttribute(firstAttr);
			}
			op.setOutput(output);
		}
	}
	
	public void visit(Product op) {
		// Output relation is size of total tuples in both relations
		Relation output = new Relation(op.getLeft().getOutput().getTupleCount() + op.getRight().getOutput().getTupleCount());
		
		// Output is the cartesian product of the two relations
		for(Attribute a : op.getLeft().getOutput().getAttributes()) {
			output.addAttribute(a);
		}
		for(Attribute a : op.getRight().getOutput().getAttributes()) {
			output.addAttribute(a);
		}
		op.setOutput(output);
	}
	
	public void visit(Join op) {
		// Output relation is minimum size of either tuples in a and tuples in b
		int attrCountA = op.getLeft().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getValueCount();
		int attrCountB = op.getRight().getOutput().getAttribute(op.getPredicate().getRightAttribute()).getValueCount();
		Relation output = new Relation(Math.min(attrCountA, attrCountB));
		
		// Add all attributes in both A and B to the output relation
		for(Attribute a : op.getLeft().getOutput().getAttributes()) {
			if(op.getRight().getOutput().getAttribute(a) != null) {
				output.addAttribute(a);
			}
		}
		op.setOutput(output);
	}
}
