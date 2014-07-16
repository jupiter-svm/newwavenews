package ru.chikov.andrey.newwavenews;

public class PostData {
	private String dateOfPost;
	private String author;
	private String text;
	private String videoTitle;
	private String page;
	private String newTag;

	PostData() {
		this.dateOfPost = "";
		this.author = "";
		this.text = "";
		this.videoTitle = "";
		this.page="";
		this.newTag="true";
	}

	PostData(String dateofPost, String author, String text, String videoTitle,
			String page, String newTag) {
		this.dateOfPost = dateofPost;
		this.author = author;
		this.text = text;
		this.videoTitle = videoTitle;
		this.page=page;
		this.newTag=newTag;
	}

	public String getDate() {
		return dateOfPost;
	}

	public String getAuthor() {
		return author;
	}

	public String getText() {
		return text;
	}

	public String getVideoTitle() {
		return videoTitle;
	}
	
	public String getPage() {
		return page;
	}
	
	public String newTag() {
		return newTag;
	}
}
