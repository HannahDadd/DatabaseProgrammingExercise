package sjdb;

import java.util.ArrayList;

public class Optimiser {
	Catalogue catalogue;
	ArrayList<Select> selectsAttrVal;
	ArrayList<Product> products;
	ArrayList<Project> projects;
	ArrayList<Scan> relations;
	ArrayList<Select> selectsAttrAttr;

	/**
	 * Take canonical query plan
	 */
	public Optimiser(Catalogue catalogue) {
		this.catalogue = catalogue;
		selectsAttrVal = new ArrayList<Select>();
		selectsAttrAttr = new ArrayList<Select>();
		products = new ArrayList<Product>();
		projects = new ArrayList<Project>();
		relations = new ArrayList<Scan>();
	}

	public Operator optimise(Operator plan) {
		findAllOperators(plan);
		ArrayList<Operator> newTree = new ArrayList<Operator>();
		Operator topOfTree = null;
		
		// Make all the relations
		for(int i = relations.size()-1; i>-1; i--) {
			try {
				Scan newRelation = new Scan(catalogue.getRelation(relations.get(i).getRelation().toString()));
				newTree.add(newRelation);
				topOfTree = newRelation;
			} catch (DatabaseException e) {
				e.printStackTrace();
			}
		}
		
		// List the attributes needed and use this to optimise projects
		ArrayList<Attribute> attributesNeeded = new ArrayList<Attribute>();
		for(Project proj : projects) {
			attributesNeeded.addAll(proj.getAttributes());
		}
		for(Select s : selectsAttrAttr) {
			// Add to attributes needed to work out projects
			attributesNeeded.add(s.getPredicate().getLeftAttribute());
			attributesNeeded.add(s.getPredicate().getRightAttribute());
		}
		
		// Loop through relations add selects above them where required, add all projects above them
		for(int i = 0; i<newTree.size(); i++) {
			// Put attr = val selects above relation they are referring to
			for(Select s : selectsAttrVal) {
					// Will be a scan
					Operator r = newTree.get(i);
					Operator opBeneathProject = r;
					try {
						// If select corresponds to this relation, add the select above the relation
						if(r.getOutput().getAttribute(s.getPredicate().getLeftAttribute()) != null) {
							// Place the select above the relation it uses
							Select newSelect = new Select(r, new Predicate(s.getPredicate().getLeftAttribute(),
									s.getPredicate().getRightValue()));
							opBeneathProject = newSelect;
						}
					} catch (IndexOutOfBoundsException e) {
						// the attribute is not in this relation, do nothing
					}
					
					// Create a project to go above the relation/select that will project only needed attrs in query
					Project proj = getProjectForRelation(attributesNeeded, (Scan) r, opBeneathProject);
					
					// Replace scan/select in tree with Project
					newTree.remove(i);
					newTree.add(i, proj);
					topOfTree = proj;
				}
		}
		
		// We should now have a list of projects with either a scan underneath or a select. Bottom of tree done.
		
		// Now add Products/Joins to tree that join the newTree elems
		// Canonical plans are always left deep, so products will be added in order
		// from bottom of tree
		if(products.size()>0) {
			// First product in array list will never have any products under it
			Product firstProduct = products.get(0);
			
			// Find the relations this product joins
			Operator lhs = firstProduct.getLeft();
			while(!(lhs instanceof Scan)) {
				// It won't be a binary operator as this is the first product in the tree
				lhs = lhs.getInputs().get(0);
			}
			
			// Find this relation in the new tree and find the above select + project if there is one
			lhs = findRelation(((Scan)lhs), newTree);
			
			// Make a list of joins to optimise joins later
			ArrayList<Join> joins = new ArrayList<Join>();
			
			// Make the first product/Join and add to array list of joins
			Operator newProductJoin = createProductJoin(lhs, firstProduct, newTree);
			topOfTree = newProductJoin;
			if(newProductJoin instanceof Join) {
				joins.add((Join) newProductJoin);
			}
			
			// Loop through products and make new products and relations from them
			for(int i = 1; i<products.size(); i++) {
				Operator op = createProductJoin(topOfTree, products.get(i), newTree);
				topOfTree = op;
				if(op instanceof Join) {
					joins.add((Join) op);
				}
			}
			
			// Add projects to top of tree
			for(Project proj : projects) {
				Project newProject = new Project(topOfTree, proj.getAttributes());
				topOfTree = newProject;
				attributesNeeded.addAll(proj.getAttributes());
			}
			
			// TODO Loop through all joins and put most restrictive join at top
			for(Join j : joins) {
			}
		}
		return topOfTree;
	}
	
	/**
	 * Add a project for a relation
	 */
	public Project getProjectForRelation(ArrayList<Attribute> attributesNeeded, Scan scan, Operator opBenathProject) {
		
		// Only project out attributes that are needed
		ArrayList<Attribute> attrs = new ArrayList<Attribute>();
		for(Attribute a : attributesNeeded) {
			for(Attribute aInRelation : scan.getRelation().getAttributes()) {
				if(a.equals(aInRelation)) {
					attrs.add(a);
				}
			}
		}
		return new Project(opBenathProject, attrs);
	}

	/**
	 * Find relation in second part of subtree and create product with second
	 * operator
	 */
	private Operator createProductJoin(Operator lhs, Product p, ArrayList<Operator> newRelations) {
		// TODO Add project above join
		// Find the relation this product applies to
		Operator currentRight = p.getRight();
		while (!(currentRight instanceof Scan)) {
			// It won't be a binary operator as no joins in rhs of tree- left deep tree
			currentRight = currentRight.getInputs().get(0);
		}
		
		// Find the corresponding relation in the new tree
		Operator rhs = findRelation(((Scan)currentRight), newRelations);
		
		// If there is a select attr = attr which corresponds to this product create a join
		// All attr=attr will correspond to a product in this syntax
		boolean isJoin = false;
		Predicate pred = null;
		for(Select s : this.selectsAttrAttr) {
			try {
				// Try and get the attribute from the relation, catch error if thrown
				if(((Scan) currentRight).getRelation().getAttribute(s.getPredicate().getRightAttribute()) != null) {
					isJoin = true;
					pred = s.getPredicate();
				}
			} catch (IndexOutOfBoundsException e) {
				// the attribute is not in this relation, do nothing
			}
		}
		Operator headOfTree = null;
		if(isJoin) {
			headOfTree = new Join(lhs, rhs, pred);
		} else {
			headOfTree = new Product(lhs, rhs);
		}
		return headOfTree;
	}
	
	/**
	 * Find a relation in the new tree from one in the old tree and return anything above it
	 * Method is only called when on right of branch or bottom of tree i.e. No joins in new tree
	 */
	private Operator findRelation(Scan oldTreeRelation, ArrayList<Operator> newTree) {
		// Loop through new trees relations and find this relation
		Operator relation = null;
		for(Operator r : newTree) {
			Operator currentRelation = r;
			// Find relation, should be under project/ project-select
			while(!(currentRelation instanceof Scan)) {
				// It won't be a binary operator as tree does not contain products
				currentRelation = currentRelation.getInputs().get(0);
			}
			// Once we've found the scan see if it's the relation we're looking for
			if(oldTreeRelation.toString().equals(currentRelation.toString())) {
				// Relation is now the top of the tree containing the scan we are looking for in the old tree
				relation = r;
			}
		}
		// Return the scan
		return relation;
	}

	/**
	 * Traverse tree to find all operators
	 */
	public void findAllOperators(Operator o) {
		// Check which operator it is
		if (o instanceof Select) {
			// See if it's attr = attr or attr = val
			if (((Select) o).getPredicate().equalsValue()) {
				selectsAttrVal.add((Select) o);
			} else {
				selectsAttrAttr.add((Select) o);
			}
		} else if (o instanceof Product) {
			products.add((Product) o);
		} else if (o instanceof Project) {
			projects.add((Project) o);
		} else if (o instanceof Scan) {
			relations.add((Scan) o);
		}

		// Traverse next part of tree
		try {
			for (Operator input : o.getInputs()) {
					findAllOperators(input);
			}
		} catch (NullPointerException e) {
			// Input was null
		}
	}
}
