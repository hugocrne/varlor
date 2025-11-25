# Data Import - User Guide

## Overview

Welcome to Varlor's data import feature! This guide will help you upload and analyze your data files quickly and easily. Whether you're working with CSV files or Excel spreadsheets, Varlor makes it simple to get your data into the system.

## Table of Contents

1. [Getting Started](#getting-started)
2. [Supported File Formats](#supported-file-formats)
3. [How to Upload a File](#how-to-upload-a-file)
4. [Understanding the Preview](#understanding-the-preview)
5. [File Requirements and Limits](#file-requirements-and-limits)
6. [Troubleshooting](#troubleshooting)
7. [Frequently Asked Questions](#frequently-asked-questions)

---

## Getting Started

### Accessing the Import Feature

1. Log in to your Varlor account at [https://app.varlor.com](https://app.varlor.com)
2. Navigate to the **Dashboard**
3. Click on **Datasets** in the sidebar
4. Click the **Import Dataset** button

You'll be taken to the import page where you can upload your data files.

---

## Supported File Formats

Varlor supports the following file formats:

### CSV Files (.csv)
- **Delimiters**: Comma, semicolon, tab, or pipe - automatically detected
- **Encoding**: UTF-8, Latin-1 (ISO-8859-1), Windows-1252 - automatically detected
- **Text escaping**: Double quotes for fields containing delimiters
- **Headers**: First row is treated as column names

**Example CSV formats supported**:
```csv
Name,Age,City
John Doe,30,New York
Jane Smith,25,Los Angeles
```

```csv
Name;Age;City
John Doe;30;New York
Jane Smith;25;Los Angeles
```

### Excel Files (.xlsx, .xls)
- **Modern Excel**: .xlsx (Excel 2007 and later)
- **Legacy Excel**: .xls (Excel 97-2003)
- **Multiple sheets**: You can choose which sheet to import
- **Formulas**: Calculated values are imported (not the formulas themselves)

---

## How to Upload a File

### Method 1: Drag and Drop (Recommended)

1. On the import page, locate the **upload zone** (the area with a dashed border)
2. **Drag your file** from your computer and **drop it** into the upload zone
3. The upload will start automatically

### Method 2: Browse for File

1. On the import page, click the **"Browse files"** button
2. Select your file using the file picker dialog
3. Click **"Open"** to start the upload

### Upload Process

Once you upload a file, you'll see three phases:

**Phase 1: Uploading**
- A progress bar shows the upload progress (0-100%)
- File is being transferred to Varlor's servers
- **Average time**: 1-5 seconds for typical files, up to 30 seconds for 100MB files

**Phase 2: Analyzing**
- A spinner appears with "Analyzing..." message
- Varlor is reading your file and detecting data types
- **Average time**: 1-5 seconds, depending on file size

**Phase 3: Preview Ready**
- The preview table appears with your data
- You can review the data before finalizing the import

### For Excel Files with Multiple Sheets

If your Excel file contains multiple sheets:

1. After upload, a **sheet selector dropdown** will appear
2. Select the sheet you want to import
3. The preview will update automatically
4. Only one sheet can be imported at a time

---

## Understanding the Preview

### Preview Table

After your file is processed, you'll see a preview table showing:

- **First 20 rows** of your data
- **All columns** from your file (scroll horizontally if needed)
- **Column headers** with detected data types

At the bottom of the table, you'll see:
> "Preview of first 20 rows out of 50,000 detected rows"

This tells you how many total rows were found in your file.

### Data Type Indicators

Each column header shows an icon and badge indicating the detected data type:

- **Text** 📝 - Text or string values (names, descriptions, IDs)
- **Number** 🔢 - Numeric values (integers, decimals, percentages)
- **Date** 📅 - Date and time values
- **Unknown** ❓ - Mixed or unrecognized data types

Varlor automatically detects these types by analyzing your data. This helps with future analysis and ensures your data is processed correctly.

### Confirm and Analyze

Once you're satisfied with the preview:

1. Review the column names and data types
2. Verify the data looks correct
3. Click the **"Confirm and analyze"** button

You'll be redirected to the dataset detail page where you can:
- View the processing status
- See complete metadata about your dataset
- Access the full data once processing is complete

---

## File Requirements and Limits

### File Size Limits

- **Maximum file size**: 100 MB per file
- Files larger than 100 MB will be rejected with an error message
- **Tip**: If you have a larger file, consider splitting it into smaller chunks or removing unnecessary columns

### Row Limits

- **Maximum rows**: 500,000 rows per file
- Files with more than 500,000 rows will be rejected
- **Tip**: If you have more data, consider filtering your data or uploading it in batches

### Upload Limits

- **Maximum uploads**: 10 uploads per hour
- This prevents system abuse and ensures good performance for all users
- If you hit this limit, please wait before uploading more files

### File Requirements

**For CSV files:**
- Must have a header row with column names
- Text fields containing delimiters should be quoted
- Encoding should be UTF-8, Latin-1, or Windows-1252
- Empty files are not allowed

**For Excel files:**
- Must contain at least one sheet with data
- First row is treated as column headers
- Empty sheets are not allowed
- Macros and VBA code are not executed

---

## Troubleshooting

### "Unable to read file"

**Possible causes:**
- File is corrupted
- File format is not supported
- File is empty or contains no data

**What to do:**
1. Try opening the file on your computer to verify it's not corrupted
2. Save the file again in a supported format (CSV or Excel)
3. Ensure the file contains actual data
4. Click **"Try again with another file"** to upload a different file

### "Unsupported format"

**Possible causes:**
- File extension is not .csv, .xlsx, or .xls
- File type doesn't match the extension (e.g., a text file renamed to .csv)

**What to do:**
1. Check your file extension
2. For CSV: Save your file as "CSV (Comma delimited) (*.csv)" in Excel
3. For Excel: Save your file as "Excel Workbook (*.xlsx)" or "Excel 97-2003 Workbook (*.xls)"
4. Try uploading again

### "File too large"

**Possible causes:**
- Your file exceeds 100 MB

**What to do:**
1. Check your file size (right-click → Properties)
2. Remove unnecessary columns to reduce file size
3. Split your data into multiple smaller files
4. Export a subset of your data (e.g., by date range)

### "Too many rows"

**Possible causes:**
- Your file contains more than 500,000 rows

**What to do:**
1. Filter your data to include only necessary rows
2. Split your data into multiple files
3. Use database exports with LIMIT clauses
4. Consider using date-based filtering to reduce row count

### "Corrupted file"

**Possible causes:**
- File was not saved properly
- File transfer was interrupted
- Disk errors during file creation

**What to do:**
1. Re-export your data from the original source
2. Save the file again
3. Try downloading a fresh copy if it's from another source
4. Check your disk for errors

### "Unrecognized encoding"

**Possible causes:**
- CSV file uses an unsupported character encoding

**What to do:**
1. Open your CSV file in a text editor or Excel
2. Save it with UTF-8 encoding:
   - In Excel: File → Save As → Tools → Web Options → Encoding → Unicode (UTF-8)
   - In Notepad: File → Save As → Encoding → UTF-8
3. Upload the newly saved file

### "Too many upload requests"

**Possible causes:**
- You've uploaded more than 10 files in the last hour

**What to do:**
1. Wait for the specified time (shown in the error message)
2. If you need to upload multiple files, space them out over time
3. Contact support if you have a legitimate need for higher limits

### Upload is very slow

**Possible causes:**
- Large file size
- Slow internet connection
- Server is busy

**What to do:**
1. Check your internet connection speed
2. Try uploading during off-peak hours
3. Consider uploading smaller files
4. Be patient - uploads up to 100 MB may take up to 30 seconds

---

## Frequently Asked Questions

### Can I upload multiple files at once?

Currently, Varlor supports uploading one file at a time. To upload multiple files, please upload them one after another.

### Can I edit my data before importing?

During the preview phase, you can only view your data. If you need to make changes, please edit your file on your computer and re-upload it.

### Can I import multiple sheets from Excel at once?

No, you can only import one sheet at a time. If you need data from multiple sheets, you'll need to import each sheet separately.

### What happens to my Excel formulas?

Excel formulas are not imported - only the calculated values are imported. This ensures data consistency and avoids formula errors.

### Can I override the detected data types?

In the current version, data type detection is automatic and cannot be overridden during import. If a type is detected incorrectly, please contact support.

### How is my data stored?

Your data is stored securely with:
- Tenant isolation (your data is separated from other users)
- Encrypted storage
- Regular backups
- Access controls (only you and authorized users in your organization can access your data)

### Can I delete uploaded datasets?

Yes! Navigate to the dataset detail page and look for the delete option. Deleting a dataset will remove both the data and the uploaded file from our servers.

### What file formats are NOT supported?

The following formats are not currently supported:
- JSON files
- XML files
- Parquet files
- Text files (plain .txt)
- Other spreadsheet formats (Google Sheets must be exported to Excel or CSV first)
- Database dumps (SQL files)

### Can I schedule automatic imports?

Scheduled imports are not available in the current version but are planned for a future update.

### How long does processing take?

Processing times depend on file size:
- **Small files** (< 1 MB): 1-2 seconds
- **Medium files** (1-10 MB): 2-5 seconds
- **Large files** (10-100 MB): 5-30 seconds
- **Very large files** (100 MB or 500k rows): Up to 30 seconds

### What if my file has special characters?

Special characters are supported as long as your file uses UTF-8, Latin-1, or Windows-1252 encoding. If you see garbled text, try re-saving your file with UTF-8 encoding.

### Can I use Varlor on mobile devices?

Yes! The data import interface is responsive and works on tablets and smartphones. However, we recommend using a desktop or laptop for the best experience, especially for large files.

### Is there a file template I can use?

Varlor works with any CSV or Excel file that follows standard formatting:
- First row contains column headers
- Each subsequent row contains data
- Consistent number of columns in each row

You don't need a special template - just use your existing files!

---

## Getting Help

### Support Resources

- **Help Center**: [https://help.varlor.com](https://help.varlor.com)
- **Email Support**: support@varlor.com
- **Live Chat**: Available in the bottom-right corner of the application

### Reporting Issues

If you encounter a problem not covered in this guide:

1. Take a screenshot of any error messages
2. Note what you were trying to do when the error occurred
3. Contact our support team with:
   - Your account email
   - Description of the issue
   - Screenshot (if applicable)
   - File details (size, format, approximate row count) - do NOT send the actual file unless requested

### Feature Requests

Have an idea to improve the data import feature?
- Submit feedback through the **Help** menu in the application
- Email us at feedback@varlor.com
- Join our community forum (link available in the Help Center)

---

## Tips for Successful Imports

### Best Practices

1. **Use clear column names** - Make your header row descriptive
2. **Clean your data first** - Remove duplicate headers, empty rows, or merged cells
3. **Check file size** - Keep files under 100 MB for faster uploads
4. **Use UTF-8 encoding** - Ensures special characters display correctly
5. **Review the preview** - Always check the preview before confirming
6. **Save your work** - Keep a backup of your original file

### Common Data Preparation Steps

Before uploading:

1. **Remove unnecessary columns** - Only include data you need
2. **Fix date formats** - Use consistent date formats (e.g., YYYY-MM-DD)
3. **Handle missing values** - Decide how to represent missing data
4. **Remove special formatting** - Excel colors, borders, and styles are not imported
5. **Flatten merged cells** - Split merged cells into individual cells

### Optimizing Upload Speed

- Use a wired internet connection when possible
- Close other applications using bandwidth
- Upload during off-peak hours if experiencing slowness
- Compress your data by removing unnecessary rows/columns

---

## What's Next?

After importing your dataset, you can:

1. **View dataset details** - See metadata, row counts, and column information
2. **Explore your data** - Use Varlor's analysis tools (coming soon)
3. **Clean your data** - Apply data cleaning pipelines (coming soon)
4. **Visualize insights** - Create charts and dashboards (coming soon)
5. **Share with your team** - Collaborate on data analysis (coming soon)

---

**Document Version**: 1.0
**Last Updated**: 2025-11-23
**For**: Varlor Data Import Feature

Need help? Contact us at support@varlor.com
