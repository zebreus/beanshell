/*****************************************************************************
 *                                                                           *
 *  This file is part of the BeanShell Java Scripting distribution.          *
 *  Documentation and updates may be found at http://www.beanshell.org/      *
 *                                                                           *
 *  Sun Public License Notice:                                               *
 *                                                                           *
 *  The contents of this file are subject to the Sun Public License Version  *
 *  1.0 (the "License"); you may not use this file except in compliance with *
 *  the License. A copy of the License is available at http://www.sun.com    * 
 *                                                                           *
 *  The Original Code is BeanShell. The Initial Developer of the Original    *
 *  Code is Pat Niemeyer. Portions created by Pat Niemeyer are Copyright     *
 *  (C) 2000.  All Rights Reserved.                                          *
 *                                                                           *
 *  GNU Public License Notice:                                               *
 *                                                                           *
 *  Alternatively, the contents of this file may be used under the terms of  *
 *  the GNU Lesser General Public License (the "LGPL"), in which case the    *
 *  provisions of LGPL are applicable instead of those above. If you wish to *
 *  allow use of your version of this file only under the  terms of the LGPL *
 *  and not to allow others to use your version of this file under the SPL,  *
 *  indicate your decision by deleting the provisions above and replace      *
 *  them with the notice and other provisions required by the LGPL.  If you  *
 *  do not delete the provisions above, a recipient may use your version of  *
 *  this file under either the SPL or the LGPL.                              *
 *                                                                           *
 *  Patrick Niemeyer (pat@pat.net)                                           *
 *  Author of Learning Java, O'Reilly & Associates                           *
 *  http://www.pat.net/~pat/                                                 *
 *                                                                           *
 *****************************************************************************/

package bsh.util;

import java.util.*;
import java.util.zip.*;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.io.*;
import java.awt.*;
import java.lang.reflect.*;
import java.util.List;

// For string related utils
import bsh.BshClassManager;
import bsh.classpath.BshClassPath;
import bsh.classpath.ClassPathListener;
import bsh.ClassPathException;
import bsh.StringUtil;
import bsh.ConsoleInterface;
import bsh.classpath.ClassManagerImpl;

/**
	A simple class browser for the BeanShell desktop.
*/
public class ClassBrowser extends JSplitPane 
	implements ListSelectionListener, ClassPathListener
{
	BshClassPath classPath;

	// GUI
	JFrame frame;
	JInternalFrame iframe;
	JList classlist, mlist, conslist;
	PackageTree ptree;
	JTextArea methodLine;
	JTree tree;
	// For JList models
	String [] packagesList;
	String [] classesList;
	Method [] methodList;
	Constructor [] consList;

	String selectedPackage;
	Class selectedClass;

	public ClassBrowser() {
		super( VERTICAL_SPLIT, true );
	}

	String [] toSortedStrings ( Collection c ) {
		List l = new ArrayList( c );
		String [] sa = (String[])(l.toArray( new String[0] ));
		return StringUtil.bubbleSort(sa);
	}

	void setClist( String packagename ) {
		this.selectedPackage = packagename;

		Set set = classPath.getClassesForPackage( packagename );
		if ( set == null )
			set = new HashSet();

		// remove inner classes and shorten class names
		List list = new ArrayList();
		Iterator it = set.iterator();
		while (it.hasNext()) {
			String cname = (String)it.next();
			if ( cname.indexOf("$") == -1 )
				list.add( BshClassPath.splitClassname( cname )[1] );
		}

		classesList = toSortedStrings(list);
		classlist.setListData( classesList );
		//setMlist( (String)classlist.getModel().getElementAt(0) );
	}

	String [] parseMethods( Method [] methods ) {
		String [] sa = new String [ methods.length ] ;
		for(int i=0; i< sa.length; i++)
			sa[i] = StringUtil.methodString( 
				methods[i].getName(), methods[i].getParameterTypes() );
		//return bubbleSort(sa);
		return sa;
	}

	String [] parseConstructors( Constructor [] constructors ) {
		String [] sa = new String [ constructors.length ] ;
		for(int i=0; i< sa.length; i++) {
			Constructor con = constructors[i];
			sa[i] = StringUtil.methodString( 
				con.getName(), con.getParameterTypes() );
		}
		//return bubbleSort(sa);
		return sa;
	}

	Constructor [] getPublicConstructors( Constructor [] constructors ) {
		Vector v = new Vector();
		for(int i=0; i< constructors.length; i++)
			if ( Modifier.isPublic(constructors[i].getModifiers()) )
				v.addElement( constructors[i] );

		Constructor [] ca = new Constructor [ v.size() ];
		v.copyInto( ca );
		return ca;
	}
	Method [] getPublicMethods( Method [] methods ) {
		Vector v = new Vector();
		for(int i=0; i< methods.length; i++)
			if ( Modifier.isPublic(methods[i].getModifiers()) )
				v.addElement( methods[i] );

		Method [] ma = new Method [ v.size() ];
		v.copyInto( ma );
		return ma;
	}

	void setMlist( String classname ) {
		if ( classname == null ) {
			mlist.setListData( new Object [] { } );
			setConslist( null );
			setClassTree( null );
			return;
		}

		Class clas;
		try {
			selectedClass = BshClassManager.classForName( 
				selectedPackage + "." + classname );
		} catch ( Exception e ) { 
			System.out.println(e);
			return;
		}
		methodList = getPublicMethods( selectedClass.getDeclaredMethods() );
		mlist.setListData( parseMethods(methodList) );
		setClassTree( selectedClass );
		setConslist( selectedClass );
	}

	void setConslist( Class clas ) {
		if ( clas == null ) {
			conslist.setListData( new Object [] { } );
			return;
		}

		consList = getPublicConstructors( clas.getDeclaredConstructors() );
		conslist.setListData( parseConstructors(consList) );
	}

	void setMethodLine( Object method ) {
		methodLine.setText( method==null ? "" : method.toString() );
	}

	void setClassTree( Class clas ) {
		if ( clas == null ) {
			tree.setModel( null );
			return;
		}
			
		MutableTreeNode bottom = null, top = null;
		DefaultMutableTreeNode up;
		do {
			up= new DefaultMutableTreeNode( clas.toString() );
			if ( top != null )
				up.add( top );
			else
				bottom = up;
			top = up;
		} while ( (clas = clas.getSuperclass()) != null );
		tree.setModel( new DefaultTreeModel(top) );

		TreeNode tn = bottom.getParent();
		if ( tn != null ) {
			TreePath tp =  new TreePath (
				((DefaultTreeModel)tree.getModel()).getPathToRoot( tn ) );
			tree.expandPath( tp );
		}
	}

	JPanel labeledPane( JComponent comp, String label ) {
		JPanel jp = new JPanel( new BorderLayout() );
		jp.add( "Center", comp );
		jp.add( "North", new JLabel(label, SwingConstants.CENTER) );
		return jp;
	}

	public void init() throws ClassPathException 
	{
		BshClassManager bcm = BshClassManager.getClassManager();
		if ( bcm == null )
			throw new ClassPathException("No Class Manager...");

		// Currently we have to cast because BshClassPath is not known by
		// the core.
		classPath = ((ClassManagerImpl)bcm).getClassPath();

	// need to add MappingFeedbackListener here
	/*
		classPath.insureInitialized( null 
			// get feedback on mapping...
			new ConsoleInterface() {
				public Reader getIn() { return null; }
				public PrintStream getOut() { return System.out; }
				public PrintStream getErr() { return System.err; }
				public void println( String s ) { System.out.println(s); }
				public void print( String s ) { System.out.print(s); }
				public void print( String s, Color color ) { print( s ); }
				public void error( String s ) { print( s ); }
			}
		);
	*/

		classPath.addListener( this );

		Set pset = classPath.getPackagesSet();

		ptree = new PackageTree( pset );
		ptree.addTreeSelectionListener( new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				TreePath tp = e.getPath();
				Object [] oa = tp.getPath();
				StringBuffer selectedPackage = new StringBuffer();
				for(int i=1; i<oa.length; i++) {
					selectedPackage.append( oa[i].toString() );
					if ( i+1 < oa.length )
						selectedPackage.append(".");
				}
				setClist( selectedPackage.toString() );
			}
		} );

		classlist=new JList();
		classlist.addListSelectionListener(this);

		mlist = new JList();
		mlist.addListSelectionListener(this);

		conslist = new JList();
		conslist.addListSelectionListener(this);

		JSplitPane methodspane = new JSplitPane(
			JSplitPane.VERTICAL_SPLIT, true, 
			labeledPane(new JScrollPane(mlist), "Methods"),
			labeledPane(new JScrollPane(conslist), "Constructors"));

		JSplitPane sp = new JSplitPane( 
			JSplitPane.HORIZONTAL_SPLIT, true, 
			labeledPane(new JScrollPane(classlist), "Classes"),
			methodspane );
		sp = new JSplitPane( 
			JSplitPane.HORIZONTAL_SPLIT, true, 
				labeledPane(new JScrollPane(ptree), "Packages"), sp);

		JPanel bottompanel = new JPanel( new BorderLayout() );
		methodLine = new JTextArea(1,60);
		methodLine.setEditable(false);
		methodLine.setLineWrap(true);
		methodLine.setWrapStyleWord(true);
		methodLine.setFont( new Font("Monospaced", Font.BOLD, 14) );
		methodLine.setMargin( new Insets(5,5,5,5) );
		methodLine.setBorder( BorderFactory.createRaisedBevelBorder() );
		bottompanel.add("North", methodLine);
		JPanel p = new JPanel( new BorderLayout() );

		tree = new JTree();
		tree.addTreeSelectionListener( new TreeSelectionListener() {
			public void valueChanged(TreeSelectionEvent e) {
				driveToClass( e.getPath().getLastPathComponent().toString() );
			}
		} );

		tree.setBorder( BorderFactory.createRaisedBevelBorder() );
		setClassTree(null);
		p.add( "Center", tree );
		bottompanel.add("Center", p );

		// give it a preferred height
		bottompanel.setPreferredSize(new java.awt.Dimension(150,150));
		
		setTopComponent( sp );
		setBottomComponent( bottompanel );
	}

	public static void main( String [] args ) 
		throws Exception
	{
		ClassBrowser cb = new ClassBrowser();
		cb.init();

		JFrame f=new JFrame("BeanShell Class Browser v1.0");
		f.getContentPane().add( "Center", cb );
		cb.setFrame( f );
		f.pack();
		f.show();
	}

	public void setFrame( JFrame frame ) {
		this.frame = frame;
	}
	public void setFrame( JInternalFrame frame ) {
		this.iframe = frame;
	}

	public void valueChanged(ListSelectionEvent e) {
		if ( e.getSource() == classlist ) {
			String classname = (String)classlist.getSelectedValue();
			setMlist( classname );
		} else
		if ( e.getSource() == mlist ) {
			int i = mlist.getSelectedIndex();
			if ( i == -1 )
				setMethodLine( null );
			else
				setMethodLine( methodList[i] );
		} else
		if ( e.getSource() == conslist ) {
			int i = conslist.getSelectedIndex();
			if ( i == -1 )
				setMethodLine( null );
			else
				setMethodLine( consList[i] );
		} 
	}

	// fully qualified classname
	public void driveToClass( String classname ) {
		String [] sa = BshClassPath.splitClassname( classname );
		String packn = sa[0];
		String classn = sa[1];

		// Do we have the package?
		if ( classPath.getClassesForPackage(packn).size()==0 )
			return;

		ptree.setSelectedPackage( packn );

		for(int i=0; i< classesList.length; i++) {
			if ( classesList[i].equals(classn) ) {
				classlist.setSelectedIndex(i);
				classlist.ensureIndexIsVisible(i);
				break;
			}
		}
	}

	public void toFront() {
		if ( frame != null )
			frame.toFront();		
		else
		if ( iframe != null )
			iframe.toFront();		
	}

	class PackageTree extends JTree 
	{
		TreeNode root;
		DefaultTreeModel treeModel;
		Map nodeForPackage = new HashMap();

		PackageTree( Collection packages ) {
			setPackages( packages );

			setRootVisible(false);
			setShowsRootHandles(false);
			setExpandsSelectedPaths(true);

			// open top level paths
			/*
			Enumeration e1=root.children();
			while( e1.hasMoreElements() ) {
				TreePath tp = new TreePath( 
					treeModel.getPathToRoot( (TreeNode)e1.nextElement() ) );
				expandPath( tp );
			}
			*/
		}

		public void setPackages( Collection packages ) {
			treeModel = makeTreeModel(packages);
			setModel( treeModel );
		}
		
		DefaultTreeModel makeTreeModel( Collection packages ) 
		{
			Map packageTree = new HashMap();

			Iterator it=packages.iterator();
			while( it.hasNext() ) {
				String pack = (String)(it.next());
				String [] sa = StringUtil.split( pack, "." );
				Map level=packageTree;
				for (int i=0; i< sa.length; i++ ) {
					String name = sa[i];
					Map map=(Map)level.get( name );

					if ( map == null ) {
						map=new HashMap();
						level.put( name, map );
					} 
					level = map;
				}
			}

			root = makeNode( packageTree, "root" );
			mapNodes(root);
			return new DefaultTreeModel( root );
		}


		MutableTreeNode makeNode( Map map, String nodeName ) 
		{
			DefaultMutableTreeNode root = 
				new DefaultMutableTreeNode( nodeName );
			Iterator it=map.keySet().iterator();
			while(it.hasNext() ) {
				String name = (String)it.next();
				Map val = (Map)map.get(name);
				if ( val.size() == 0 ) {
					DefaultMutableTreeNode leaf = 
						new DefaultMutableTreeNode( name );
					root.add( leaf );
				} else {
					MutableTreeNode node = makeNode( val, name );
					root.add( node );
				}
			}
			return root;
		}

		/**
			Map out the location of the nodes by package name.
			Seems like we should be able to do this while we build above...
			I'm tired... just going to do this.
		*/
		void mapNodes( TreeNode node ) {
			addNodeMap( node );

			Enumeration e = node.children();
			while(e.hasMoreElements()) {
				TreeNode tn = (TreeNode)e.nextElement();
				mapNodes( tn );
			}
		}

		/**
			map a single node up to the root
		*/
		void addNodeMap( TreeNode node ) {

			StringBuffer sb = new StringBuffer();
			TreeNode tn = node;
			while( tn != root ) {
				sb.insert(0, tn.toString() );
				if ( tn.getParent() != root )
					sb.insert(0, "." );
				tn = tn.getParent();
			}
			String pack = sb.toString();

			nodeForPackage.put( pack, node );
		}

		void setSelectedPackage( String pack ) {
			DefaultMutableTreeNode node = 
				(DefaultMutableTreeNode)nodeForPackage.get(pack);
			if ( node == null )
				return;

			TreePath tp = new TreePath(treeModel.getPathToRoot( node ));
			setSelectionPath( tp );
			setClist( pack );

			scrollPathToVisible( tp );
		}

	}

	public void classPathChanged() {
		Set pset = classPath.getPackagesSet();
		ptree.setPackages( pset );
		setClist(null);
	}

}
