package fi.hel.verkkokauppa.payment.api.data;

public class PaymentMethodDto {
	private String name;
	private String code;
	private String group;
	private String img;

	public PaymentMethodDto(String name, String code, String group, String img) {
		this.name = name;
		this.code = code;
		this.group = group;
		this.img = img;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getImg() {
		return img;
	}

	public void setImg(String img) {
		this.img = img;
	}
}
