package sjdb;

import java.util.Map;
import java.util.TreeMap;

public class Optimiser {
	Catalogue catalogue;

	/**
	 * Take canonical query plan 
	 */
	public Optimiser(Catalogue catalogue) {
		this.catalogue = catalogue;
	}

	public Operator optimise(Operator plan) {
		// Loop through query plan
		// Store selects to put them at bottom. Treemap is sorted hashmap
		Map<Integer, Operator> selects = new TreeMap<Integer, Operator>();
		for(Operator o : plan.getInputs()) {
			// If it's a select move operator down tree
			if(o instanceof Select) {
				// Work out no. of tuples of select
				((Select) o).getPredicate().getLeftAttribute().getName();
			} else if (o instanceof Product) {
				
			} else if (o instanceof Project) {
				
			} else if (o instanceof Join) {
				
			} else if (o instanceof Scan) {
				
			}
		}
		// Re-order subtrees to put most restrictive selects (fewest tuples) at the bottom
		return plan;
	}
}
