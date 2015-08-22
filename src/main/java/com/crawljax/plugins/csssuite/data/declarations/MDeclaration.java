package com.crawljax.plugins.csssuite.data.declarations;

/**
 * Created by axel on 6/9/2015.
 *
 * Represents a style declaration that is contained inside a CSS selector
 * A declaration has a name, value and may be important
 */
public class MDeclaration
{
	protected final String _name;
	private final String _originalValue;
	private final String _w3cError;
	private final boolean _isIgnored;
	private final boolean _isFaulty;
	private final boolean _isImportant;
	private final String _nameVendorPrefix;
	private final String _valueVendorPrefix;
	private final int _order;

	protected String _normalizedValue;
	private String _status;
	private boolean _isEffective;
	private boolean _isInvalidUndo;
	private boolean _invalidUndoSet;


	/**
	 * Default constructor
	 * @param name
	 * @param value
	 * @param isImportant
	 * @param w3cError
	 */
	public MDeclaration(String name, String value, boolean isImportant, String w3cError, int order)
	{
		_name = name;
		_originalValue = value.trim();
		_normalizedValue = value.trim();
		_status = "notset";
		_isImportant = isImportant;
		_w3cError = w3cError;
		_isInvalidUndo = false;
		_invalidUndoSet = false;
		_order = order;

		if(name.contains("-moz-") || name.contains("-webkit-") || name.contains("-ms-") || name.contains("-o-") || name.contains("-khtml-"))
		{
			_nameVendorPrefix = "-" + name.split("-")[1] + "-";
		}
		else
		{
			_nameVendorPrefix = "";
		}

		if(value.contains("-moz-"))
		{
			_valueVendorPrefix = "-moz-";
		}
		else if(value.contains("-webkit-"))
		{
			_valueVendorPrefix = "-webkit-";
		}
		else if(value.contains("-ms-"))
		{
			_valueVendorPrefix = "-ms-";
		}
		else if(value.contains("-o-"))
		{
			_valueVendorPrefix = "-o-";
		}
		else if(value.contains("-khtml-"))
		{
			_valueVendorPrefix = "-khtml-";
		}
		else
		{
			_valueVendorPrefix = "";
		}

		_isIgnored = !w3cError.isEmpty() || !_valueVendorPrefix.isEmpty() || value.contains("-gradient") || value.contains("progid:") || name.startsWith("_");
		_isFaulty = value.trim().isEmpty();
	}


	/**
	 * Constructor for declaration without error (used in normalizer plug-in and tests)
	 * @param name
	 * @param value
	 * @param isImportant
	 */
	public MDeclaration(String name, String value, boolean isImportant, int order)
	{
		this(name, value, isImportant, "", order);
	}


	/**
	 * Constructor for declaration without error and optional effectiveness, used in normalization
	 * @param name
	 * @param value
	 * @param isImportant
	 */
	public MDeclaration(String name, String value, boolean isImportant, boolean isEffective, int order)
	{
		this(name, value, isImportant, "", order);
		_isEffective = isEffective;
	}


	/**
	 * Full copy constructor
	 * @param declaration
	 */
	public MDeclaration(MDeclaration declaration)
	{
		_name = declaration.getName();
		_originalValue = declaration.getOriginalValue();
		_normalizedValue = declaration.getValue();
		_status = declaration.getStatus();
		_isEffective = declaration.isEffective();
		_isImportant = declaration.isImportant();
		_isIgnored = declaration.isIgnored();
		_isInvalidUndo = declaration.isInvalidUndo();
		_w3cError = declaration.getW3CError();
		_nameVendorPrefix = declaration.getNameVendor();
		_valueVendorPrefix = declaration.getValueVendor();
		_order = declaration.getOrder();
		_isFaulty = declaration.isFaulty();
	}


	/** Getter */
	public String getName()
	{
		return _name;
	}

	/** Getter */
	public String getOriginalValue()
	{
		return _originalValue;
	}

	/** Getter */
	public String getW3CError()
	{
		return _w3cError;
	}

	/** Getter */
	public boolean isIgnored() { return _isIgnored; }

	/** Getter */
	public boolean isImportant()
	{
		return _isImportant;
	}

	/** Getter */
	public boolean isFaulty() { return _isFaulty; }

	/** Getter */
	public String getNameVendor() { return _nameVendorPrefix; }

	/** Getter */
	public String getValueVendor() { return _valueVendorPrefix; }

	/** Getter */
	public int getOrder() { return _order; }

	/** Getter */
	public String getValue() { return _normalizedValue;	}

	/** Getter */
	public String getStatus()
	{
		return _status;
	}

	/** Getter */
	public boolean isEffective()
	{
		return _isEffective;
	}

	/** Getter */
	public boolean isInvalidUndo() { return _isInvalidUndo; }

	/** Getter */
	public String getFullValue()
	{
		return _normalizedValue + (_isImportant ? " !important" : "");
	}

	/**
	 * @param value a normalized value for this MDeclaration
	 */
	public void setNormalizedValue(String value)
	{
		_normalizedValue = value;
	}


	/**
	 * @param effective mark this MDeclaration as effective or ineffectiv
	 */
	public void setEffective(boolean effective)
	{
		_isEffective = effective;
	}


	/**
	 *
	 * @param status
	 */
	public void setStatus(String status)
	{
		_status = status;
	}


	/**
	 *
	 */
	public void setInvalidUndo(boolean invalid)
	{
		if(_invalidUndoSet && !_isInvalidUndo)
			return;

		if(!_invalidUndoSet)
			_invalidUndoSet = true;

		_isInvalidUndo = invalid;
	}


	/**
	 * @param otherDeclaration the other declaration besides which this one may co-exist
	 * @return false, this MDeclaration may never coexist with another MDeclaration in the same MSelector
	 */
	public boolean allowCoexistence(MDeclaration otherDeclaration)
	{
		return false;
	}


	/**
	 * Transform this property into valid CSS syntax
	 * @return valid CSS syntax
	 */
	public String print()
	{
		return String.format("{ %s : %s %s }", _name, getFullValue(), (_isEffective ? "Effective" : "Ineffective"));
	}


	/**
	 * @return a short string that may be used as a key in HashMap comparisons
	 */
	public String asKey()
	{
		return String.format("%s-%s", _name, getFullValue());
	}

	@Override
	public String toString()
	{
		return String.format("%s: %s;", _name, getFullValue());
	}
}

