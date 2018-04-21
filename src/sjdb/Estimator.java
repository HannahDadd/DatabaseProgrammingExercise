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
			// See if that attribute is in relation + add it to output relation
			for(Attribute aInInput : input.getAttributes()) {
				if(a.getName().equals(aInInput.getName())) {
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
			// Get attribute on previous operator
			int valueCount = op.getInput().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getValueCount();
			
			// Create an output relation with no. of tuples in R over no. of value in attr a in R
			Relation output = new Relation(op.getInput().getOutput().getTupleCount()/valueCount);
			
			// Set predicate value count to 1, others just add as is
			for(Attribute a : op.getInput().getOutput().getAttributes()) {
				if(a.getName() == op.getPredicate().getLeftAttribute().getName()) {
					Attribute attr = new Attribute(a.getName(), 1);
					output.addAttribute(attr);
				} else {
					output.addAttribute(a);
				}
			}
			op.setOutput(output);
		} else {
			// attr = attr			
			int valueCountMax = Math.max(op.getInput().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getValueCount(),
					op.getInput().getOutput().getAttribute(op.getPredicate().getRightAttribute()).getValueCount());
			Relation output = new Relation(op.getInput().getOutput().getTupleCount()/valueCountMax);
			
			// Add attributes to output relation
			for(Attribute a : op.getInput().getOutput().getAttributes()) {
				if(op.getInput().getOutput().getAttribute(op.getPredicate().getRightAttribute()).getName().equals(a.getName()) 
						|| op.getInput().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getName().equals(a.getName())) {
					Attribute newA = new Attribute(
							a.getName(), Math.min(op.getInput().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getValueCount(), 
									op.getInput().getOutput().getAttribute(op.getPredicate().getRightAttribute()).getValueCount()));
					output.addAttribute(newA);
				} else {
					// If value count less than tuple count add it in, otherwsie adjust value count
					if(a.getValueCount() > op.getInput().getOutput().getTupleCount()) {
						Attribute newA = new Attribute(a.getName(), op.getInput().getOutput().getTupleCount());
						output.addAttribute(newA);
					} else {
						output.addAttribute(a);
					}
				}
			}
			op.setOutput(output);
		}
	}
	
	public void visit(Product op) {
		// Output relation is size of total tuples in both relations
		Relation output = new Relation(op.getLeft().getOutput().getTupleCount() * op.getRight().getOutput().getTupleCount());
		
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
		// Output value is minimum size of either tuples in a and tuples in b
		int attrCountA = op.getLeft().getOutput().getAttribute(op.getPredicate().getLeftAttribute()).getValueCount();
		int attrCountB = op.getRight().getOutput().getAttribute(op.getPredicate().getRightAttribute()).getValueCount();
		int values = Math.min(attrCountA, attrCountB);
		
		// Relation tuple count is total tuples in each relation divided by maximum attr count
		Relation output = new Relation(op.getLeft().getOutput().getTupleCount()*op.getRight().getOutput().getTupleCount()/
				Math.max(attrCountA, attrCountB));
		
		// Add attributes to output relation
		output = addAttributes(op.getLeft().getOutput(), op.getPredicate().getLeftAttribute(), values, output);
		output = addAttributes(op.getRight().getOutput(), op.getPredicate().getRightAttribute(), values, output);
		
		op.setOutput(output);
	}
	
	/**
	 * Add attributes to output for each side of predicate
	 */
	public Relation addAttributes(Relation r, Attribute attr, int value, Relation output) {
		for(Attribute a : r.getAttributes()) {
			if(r.getAttribute(attr).getName().equals(a.getName())) {
				Attribute newA = new Attribute(
						a.getName(), value);
				output.addAttribute(newA);
			} else {
				// If value count less than tuple count add it in, otherwsie adjust value count
				if(a.getValueCount() > output.getTupleCount()) {
					Attribute newA = new Attribute(a.getName(), output.getTupleCount());
					output.addAttribute(newA);
				} else {
					output.addAttribute(a);
				}
			}
		}
		return output;
	}
}
