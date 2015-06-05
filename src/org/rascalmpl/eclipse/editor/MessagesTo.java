package org.rascalmpl.eclipse.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.rascalmpl.values.uptr.ITree;
import org.rascalmpl.values.uptr.TreeAdapter;

public class MessagesTo {
	
	MessagesTo(int defaultSeverity,Map<String,Integer> severityMap){
		this.severityMap = severityMap;
		this.defaultSeverity = defaultSeverity;
	}
	
	private Map<String,Integer> severityMap;
	private int defaultSeverity;
	

	
	public void process(final ITree parseTree, IMessageHandler handler) {
		if (parseTree != null) {
			processMarkers(parseTree, handler); 
		}
}

	private void processMarkers(ITree tree, IMessageHandler handler) {
		if (TreeAdapter.isAppl(tree) && !TreeAdapter.isLexical(tree)) {
			IValue anno = tree.asAnnotatable().getAnnotation("message");
			if (anno != null && anno.getType().isAbstractData() && anno.getType().getName().equals("Message")) {
				IConstructor message = (IConstructor) anno;
				ISourceLocation loc = TreeAdapter.getLocation(tree);
				processMessage(message, loc, handler);
			}
			
			anno = tree.asAnnotatable().getAnnotation("messages");
			
			if (anno != null && anno.getType().isSet()) {
				process((ISourceLocation) tree.asAnnotatable().getAnnotation("loc"),  (ISet) anno, handler);
				return; // we do not recurse if we found messages (for efficiency)
			}
			
			for (IValue child : TreeAdapter.getArgs(tree)) {
				processMarkers((ITree) child, handler);
			}
		}
		else if (TreeAdapter.isAmb(tree)) {
			for (IValue alt : TreeAdapter.getAlternatives(tree)) {
				processMarkers((ITree) alt, handler);
			}
		}
	}

	public void process(ISourceLocation treeLoc, ISet set, IMessageHandler handler) {
		Type elemType = set.getType().getElementType();

		if (elemType.isAbstractData() && elemType.getName().equals("Message")) {

			for (IValue messagev : ((ISet) set)) {
				IConstructor message = (IConstructor)messagev;
				ISourceLocation loc = (ISourceLocation) message.get(1);
				
				if (loc.getPath().equals(treeLoc.getPath()))
					processMessage(message, loc, handler);
			}
		}
	}
	
	private void processMessage(IConstructor marker, ISourceLocation loc, IMessageHandler handler)  {
		int severity;
		if(severityMap.containsKey(marker.getName())){
			severity = severityMap.get(marker.getName());
		} else {
			severity = defaultSeverity;
		}

		String msg = ((IString) marker.get(0)).getValue();
		Map<String,Object> attrs = new HashMap<String,Object>();
		attrs.put(IMarker.SEVERITY, severity);
		attrs.put(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);

		handler.handleSimpleMessage(msg, loc.getOffset(), loc.getOffset() + loc.getLength(), loc.getBeginColumn(), loc.getEndColumn(), loc.getBeginLine(), loc.getEndLine(), attrs);
	}
	
	public int compareTo(IModelListener o) {
		return 0;
	}
}
