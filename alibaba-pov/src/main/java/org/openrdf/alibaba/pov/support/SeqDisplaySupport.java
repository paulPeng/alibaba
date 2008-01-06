package org.openrdf.alibaba.pov.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openrdf.alibaba.core.Property;
import org.openrdf.alibaba.exceptions.AlibabaException;
import org.openrdf.alibaba.pov.DisplayBehaviour;
import org.openrdf.alibaba.pov.SeqDisplay;
import org.openrdf.alibaba.vocabulary.POV;
import org.openrdf.concepts.rdfs.Container;
import org.openrdf.elmo.annotations.rdf;

/**
 * Combines each property value into an Object array. This is useful with a
 * {@link MessageFormatDisplay}.
 * 
 * @author James Leigh
 * 
 */
@rdf(POV.NS + "SeqDisplay")
public class SeqDisplaySupport extends DisplaySupport implements
		DisplayBehaviour {
	private SeqDisplay display;

	public SeqDisplaySupport(SeqDisplay display) {
		super(display);
		this.display = display;
	}

	@Override
	public Collection<?> getValuesOf(Object resource) throws AlibabaException {
		List<Object> list = new ArrayList<Object>();
		for (Property prop : display.getPovProperties()) {
			Object value = prop.getValueOf(resource);
			if (value instanceof Collection) {
				Collection<?> set = (Collection<?>) value;
				int size = set.size();
				if (size == 0) {
					list.add(null);
				} else if (size == 1) {
					list.addAll(set);
				} else {
					list.add(set.toArray()[0]);
				}
			} else {
				list.add(value);
			}
		}
		List<Object[]> result = new ArrayList<Object[]>();
		result.add(list.toArray());
		return result;
	}

	@Override
	public void setValuesOf(Object resource, Collection<?> values)
			throws AlibabaException {
		if (!getValuesOf(resource).equals(values)) {
			Container<Property> props = display.getPovProperties();
			assert values.size() == 1 : values;
			for (Object value : values) {
				assert value instanceof Object[] : value;
				Object[] ar = (Object[]) value;
				for (int i = 0; i < ar.length; i++) {
					Property prop = props.get(i);
					prop.setValueOf(resource, ar[i]);
				}
			}
		}
	}
}
