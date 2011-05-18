package org.rascalmpl.eclipse.library.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.Category;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.services.IServiceLocator;
import org.rascalmpl.eclipse.terms.TermLanguageRegistry;
import org.rascalmpl.interpreter.result.ICallableValue;
import org.rascalmpl.values.ValueFactoryFactory;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;


public class NonRascalMenuContributionItem extends CompoundContributionItem {
	
	private static String NON_RASCAL_CONTRIBUTION_COMMAND_CATEGORY = "org.rascalmpl.eclipse.library.util.NRCMCC";
	private static String NON_RASCAL_CONTRIBUTION_COMMAND_PREFIX = "org.rascalmpl.eclipse.library.util.NRCMCP";
	private final static TypeFactory TF = TypeFactory.getInstance();
	private final static IValueFactory VF = ValueFactoryFactory.getValueFactory();

	
    /**
     * Creates a compound contribution item with a <code>null</code> id.
     */
    public NonRascalMenuContributionItem() {
        super();
    }

    /**
     * Creates a compound contribution item with the given (optional) id.
     *
     * @param id the contribution item identifier, or <code>null</code>
     */
    public NonRascalMenuContributionItem(String id) {
        super(id);
    }
    
    private class ContributionCacheItem {
    	public ISet rascalContributions;
    	public List<String> eclipseContributionsIds;
    }
    private static Map<String, ContributionCacheItem> contributionCache = new ConcurrentHashMap<String, ContributionCacheItem>();
    
	
	@Override
	protected IContributionItem[] getContributionItems() {
		String currentEditorId = getCurrentEditorID();
		if (currentEditorId.isEmpty()) {
			return new IContributionItem[0];
		}
		ISet contribs = TermLanguageRegistry.getInstance().getNonRascalContributions(currentEditorId);
		if (contribs == null) {
			return new IContributionItem[0];
		}
		
		ContributionCacheItem cachedItemIds = contributionCache.get(currentEditorId);
		List<String> contributionItemIds;
		if (cachedItemIds != null && cachedItemIds.rascalContributions == contribs) {
			contributionItemIds = cachedItemIds.eclipseContributionsIds;
		}
		else {
			contributionItemIds = generateContributions(contribs);
			
			// updating the cache
			cachedItemIds = new ContributionCacheItem();
			cachedItemIds.rascalContributions = contribs;
			cachedItemIds.eclipseContributionsIds = contributionItemIds;
			contributionCache.put(currentEditorId, cachedItemIds);
		}
		// we cannot cache this because eclipse disposes these menu items.
		IContributionItem[] result = new IContributionItem[contributionItemIds.size()];
		IServiceLocator serviceLocator = getServiceLocator();
		for (int i = 0; i < contributionItemIds.size(); i++ ) {
			CommandContributionItemParameter newCommandParams = new CommandContributionItemParameter(
					serviceLocator, null, contributionItemIds.get(i), CommandContributionItem.STYLE_PUSH);
			result[i] =  new CommandContributionItem(newCommandParams);
		}
	    return result;
	}

	private List<String> generateContributions(ISet contribs) {
		ICommandService cmdService = getCommandService();
		IHandlerService handlerService = getHandlerService();
		Category defaultCategory = getDefaultCategory(cmdService);
		List<String> result = new ArrayList<String>();
		for (IValue contrib : contribs) {
			IConstructor node = (IConstructor) contrib;
			if (node.getName().equals("popup")) {
				result.add(contribute(defaultCategory, cmdService, handlerService, (IConstructor)node.get("menu")));
			}
		}
		return result;
	}

	private String contribute(Category defaultCategory, ICommandService cmdService, IHandlerService handlerService, IConstructor menu) {

		String label = ((IString) menu.get("label")).getValue();
		if (menu.getName().equals("edit")) {
			throw new RuntimeException("Edit is not support by non rascal windows");
		}
		else if (menu.getName().equals("click")) {
			//Because we are not sure a label will not break the characters in a command id, we encode it.
			String commandId = NON_RASCAL_CONTRIBUTION_COMMAND_PREFIX + encodeLabel(label);
			Command newCommand = cmdService.getCommand(commandId);
			if (!newCommand.isDefined()) {
				newCommand.define(label, "A non rascal contribution", defaultCategory);
			}
			final ICallableValue func = (ICallableValue) menu.get("click");
			IHandler handler = new AbstractHandler() {
				@Override
				public Object execute(ExecutionEvent event) throws ExecutionException {
					ITextSelection selection = (ITextSelection)HandlerUtil.getCurrentSelection(event);
					IEditorPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
					String fileName = activeEditor.getEditorInput().getName();
					
					ISourceLocation selectedLine = VF.sourceLocation(fileName, selection.getStartLine() ,selection.getLength(),  -1, -1, -1, -1);
					if (selectedLine != null) {
						func.call(new Type[] { TF.sourceLocationType() }, new IValue[] {selectedLine });
					}
					return null;
				}
				
			};
			newCommand.setHandler(handler);
			return commandId;
			
		}
		else {
			throw new NotImplementedException();
		}
	}
	

	private String encodeLabel(String label) {
		try {
			return URLEncoder.encode(label, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return label;
		}
		
	}

	private String getCurrentEditorID() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getEditorSite().getId();
	}

	private IServiceLocator getServiceLocator() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}
	   
	private IHandlerService getHandlerService() {
		return (IHandlerService) getServiceLocator().getService(IHandlerService.class);
	}
	
	private ICommandService getCommandService() {
		return (ICommandService)getServiceLocator().getService(ICommandService.class);
	}

	private Category getDefaultCategory(ICommandService cmdService) {
		Category defaultCategory = cmdService.getCategory(NON_RASCAL_CONTRIBUTION_COMMAND_CATEGORY);
		if (!defaultCategory.isDefined()) {
			defaultCategory.define("Non Rascal Contributions", "A category for non rascal contributions");
		}
		return defaultCategory;
	}

	

}
