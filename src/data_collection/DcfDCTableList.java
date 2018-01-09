package data_collection;

import java.util.ArrayList;

public class DcfDCTableList extends ArrayList<DcfDCTable> implements IDcfDCTableList {

	private static final long serialVersionUID = -2547404158763272542L;

	@Override
	public boolean addElem(IDcfDCTable elem) {
		return super.add((DcfDCTable) elem);
	}

	@Override
	public IDcfDCTable create() {
		return new DcfDCTable();
	}

	@Override
	public IDcfCatalogueConfig createConfig() {
		return new DcfCatalogueConfig();
	}

}
