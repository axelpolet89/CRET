package com.crawljax.plugins.csssuite.data.properties;

/**
 * Represents a style declaration or CSS property that is contained inside a CSS rule
 * A property has a name, value and may be important
 * After analysis, a property may be overridden, thereby rendered ineffective
 */
public class MProperty
{
	protected final String _name;
	private final String _originalValue;
	private final String _w3cError;
	private final boolean _isIgnored;
	private final boolean _isImportant;
	private final String _allowedWith;

	protected String _normalizedValue;
	private String _status;

	private boolean _isEffective;
	private boolean _isInvalidUndo;

	/**
	 * Default constructor
	 * @param name
	 * @param value
	 * @param isImportant
	 * @param w3cError
	 */
	public MProperty(String name, String value, boolean isImportant, String w3cError)
	{
		_name = name;
		_originalValue = value.trim();
		_normalizedValue = value.trim();
		_status = "notset";
		_isImportant = isImportant;
		_w3cError = w3cError;
		_isIgnored = !w3cError.isEmpty();
		_isInvalidUndo = false;
		_allowedWith = "";
	}


	/**
	 * Constructor for property without error (used in normalizer plug-in and tests)
	 * @param name
	 * @param value
	 * @param isImportant
	 */
	public MProperty(String name, String value, boolean isImportant)
	{
		this(name, value, isImportant, "");
	}

	/** Getter */
	public String GetName()
	{
		return _name;
	}

	/** Getter */
	public String GetOriginalValue()
	{
		return _originalValue;
	}

	/** Getter */
	public String GetW3cError()
	{
		return _w3cError;
	}

	/** Getter */
	public boolean IsIgnored() { return _isIgnored; }

	/** Getter */
	public String GetValue()
	{
		return _normalizedValue;
	}

	/** Getter */
	public String GetStatus()
	{
		return _status;
	}

	/** Getter */
	public boolean IsImportant()
	{
		return _isImportant;
	}

	/** Getter */
	public boolean IsEffective()
	{
		return _isEffective;
	}

	/** Getter */
	public boolean IsInvalidUndo()
	{
		return _isInvalidUndo;
	}


	/**
	 *
	 * @param otherProperty
	 * @return
	 */
	public boolean AllowCoexistence(MProperty otherProperty)
	{
		return false;
	}

	/**
	 *
	 * @param value
	 */
	public void SetNormalizedValue(String value)
	{
		_normalizedValue = value;
	}


	/**
	 *
	 * @param effective
	 */
	public void SetEffective(boolean effective)
	{
		_isEffective = effective;
	}


	/**
	 *
	 * @param status
	 */
	public void SetStatus(String status)
	{
		_status = status;
	}


	/**
	 *
	 */
	public void SetInvalidUndo()
	{
		_isInvalidUndo = true;
	}


	/**
	 *
	 * @return
	 */
	public int ComputeSizeBytes()
	{
		return (_name.getBytes().length+ _normalizedValue.getBytes().length);
	}


	/**
	 * Transform this property into valid CSS syntax
	 * @return valid CSS syntax
	 */
	public String Print()
	{
		String result = _name + ": " + _normalizedValue;

		if(_isImportant)
			return result + " !important;";

		return result + ";";
	}


	/**
	 * @return a short string that may be used as a key in HashMap comparisons
	 */
	public String AsKey()
	{
		String result = _name + "-" + _normalizedValue;
		if(_isImportant)
			return result + "-" + "!";

		return result;
	}

	@Override
	public String toString()
	{
		return "{ " + _name + " : " + _normalizedValue + " " + (_isImportant ? "!important ": "") + (_isEffective ? "Effective" : "Ineffective") + " }";
	}
}
