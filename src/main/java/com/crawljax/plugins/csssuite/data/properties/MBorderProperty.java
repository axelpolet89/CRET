package com.crawljax.plugins.csssuite.data.properties;

/**
 * An extension to MProperty, used on outline and border properties
 * which is used to determine whether a property name represents a subset of another property
 * For instance, a border-top-width is a subset of border-width
 */
public class MBorderProperty extends MProperty
{
	private final String _allowedWith;

	public MBorderProperty(String name, String value, boolean isImportant, int order, String allowedWith)
	{
		super(name, value, isImportant, "", order);
		_allowedWith = allowedWith;
	}


	/**
	 *
	 * @param otherProperty
	 * @return
	 */
	@Override
	public boolean AllowCoexistence(MProperty otherProperty)
	{
		String name = otherProperty.GetName();

		if(_name.equals(name) || _allowedWith.contains(name))
		{
			if(!_normalizedValue.equals(otherProperty.GetValue()))
			{
				return true;
			}
		}

		return false;
	}
}
