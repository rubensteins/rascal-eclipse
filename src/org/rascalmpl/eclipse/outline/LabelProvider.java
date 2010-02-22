package org.rascalmpl.eclipse.outline;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.services.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.rascalmpl.ast.AbstractAST;
import org.rascalmpl.ast.Declaration;
import org.rascalmpl.ast.FunctionDeclaration;
import org.rascalmpl.ast.Module;
import org.rascalmpl.ast.Signature;
import org.rascalmpl.ast.Variant;
import org.rascalmpl.ast.Declaration.Variable;
import org.rascalmpl.eclipse.outline.TreeModelBuilder.Group;
import org.rascalmpl.values.uptr.TreeAdapter;

public class LabelProvider implements ILabelProvider, ILanguageService  {
	private Set<ILabelProviderListener> fListeners = new HashSet<ILabelProviderListener>();

//	private static ImageRegistry sImageRegistry = Activator.getInstance()
//			.getImageRegistry();
//
//	private static Image DEFAULT_IMAGE = sImageRegistry
//			.get(IRascalResources.RASCAL_DEFAULT_IMAGE);
//
//	@Override
//	public Image getImage(Object element) {
//		return DEFAULT_IMAGE;
//	}

	public Image getImage(Object element) {
		return null;
	}
	
	public String getText(Object element) {
		if (element instanceof ModelTreeNode) {
			ModelTreeNode node = (ModelTreeNode) element;
			
			Object node2 = node.getASTNode();
			
			if (node2 instanceof AbstractAST) {
				return getLabelFor((AbstractAST) node2);
			}
			else if (node2 instanceof IConstructor) {
				return getLabelFor((IConstructor) node2);
			}
			else if (node2 instanceof Group<?>) {
				return getLabelFor((Group<?>) node2);
			}
		}
		else if (element instanceof Group<?>) {
			return getLabelFor((Group<?>) element);
		}
		else if (element instanceof IConstructor) {
			return getLabelFor((IConstructor) element);
		}
		return "***";
	}
	
	private String getLabelFor(Group<?> group) {
		return group.getName();
	}

	private String getLabelFor(AbstractAST node2) {
		String result;
		
		if (node2 instanceof Module) {
			result = ((Module) node2).getHeader().toString();
		}
		else if (node2 instanceof Declaration.Function) {
			Signature signature = ((Declaration.Function) node2).getFunctionDeclaration().getSignature();
			result = signature.getName().toString() + signature.getParameters().toString();
		}
		else if (node2 instanceof FunctionDeclaration) {
			Signature signature = ((Declaration.Function) node2).getFunctionDeclaration().getSignature();
			result = signature.getName().toString() + signature.getParameters().toString();
		}
		else if (node2 instanceof org.rascalmpl.ast.Variable) {
			org.rascalmpl.ast.Variable v = (org.rascalmpl.ast.Variable) node2;
			result = v.getName().toString();
		}
		else if (node2 instanceof Declaration.Variable) {
			Declaration.Variable var = (Variable) node2;
			result = var.getName() + ": " + var.getType();
		}
		else if (node2 instanceof Declaration.Data) {
			result = ((Declaration.Data) node2).toString();
		}
		else if (node2 instanceof Declaration.DataAbstract) {
			result = ((Declaration.DataAbstract) node2).toString();
		}
		else if (node2 instanceof Declaration.Rule) {
			result = "rule " + ((Declaration.Rule) node2).getName().toString();
		}
		else if (node2 instanceof Declaration.Alias) {
			result = ((Declaration.Alias) node2).getUser().toString();
		}
		else if (node2 instanceof Variant.NAryConstructor) {
			Variant v = (Variant) node2;
			result = v.getName() + "(" + v.getArguments() + ")"; 
		}
		else {
		    result = node2.toString();
		}
		return result.replaceAll("\n", " ").trim();
	}

	private String getLabelFor(IConstructor node) {
		return TreeAdapter.yield(node);
	}

	public void addListener(ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	public void dispose() {
	}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		fListeners.remove(listener);
	}
}