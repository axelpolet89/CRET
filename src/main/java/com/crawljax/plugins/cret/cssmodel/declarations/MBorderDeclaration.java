package com.crawljax.plugins.cret.cssmodel.declarations;

/**
 * Created by axel on 6/9/2015.
 *
 * An extension to MDeclaration, used on outline and border declarations
 * which is used to determine whether a declaration name represents a subset of another declaration
 * For example, a border-top-width is a subset of border-width
 */
public class MBorderDeclaration extends MDeclaration
{
	private final String _allowedWith;

	public MBorderDeclaration(String name, String value, boolean isImportant, int order, String allowedWith)
	{
		super(name, value, isImportant, "", order);
		_allowedWith = allowedWith;
	}


	/**
	 *
	 * @param otherDeclaration
	 * @return
	 */
	@Override
	public boolean allowCoexistence(MDeclaration otherDeclaration)
	{
		String name = otherDeclaration.getName();

		if(_name.equals(name) || _allowedWith.contains(name))
		{
			if(!_normalizedValue.equals(otherDeclaration.getValue()))
			{
				return true;
			}
		}

		return false;
	}
}
