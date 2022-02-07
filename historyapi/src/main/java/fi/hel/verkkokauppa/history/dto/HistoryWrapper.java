package fi.hel.verkkokauppa.history.dto;

import fi.hel.verkkokauppa.common.history.dto.HistoryDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HistoryWrapper {
	public HistoryWrapper(List<HistoryDto> histories) {
		this.histories = histories;
	}

	private List<HistoryDto> histories;
}