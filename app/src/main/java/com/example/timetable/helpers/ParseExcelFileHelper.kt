package com.example.timetable.helpers

import android.util.Log
import com.example.timetable.database.Class
import kotlinx.coroutines.yield
import org.apache.poi.openxml4j.exceptions.InvalidFormatException
import org.apache.poi.openxml4j.opc.OPCPackage
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.xmlbeans.impl.piccolo.io.FileFormatException
import java.io.InputStream
import java.util.*
import kotlin.NoSuchElementException

class ParseExcelFileHelper {

    companion object {
            private const val DAY_OF_WEEK_COLUMN_INDEX = 0
            private const val CLASS_NUMBER_COLUMN_INDEX = 1
            private const val START_TIME__COLUMN_INDEX = 2
            private const val END_TIME_COLUMN_INDEX = 3
            private const val CLASS_WEEK_COLUMN_INDEX = 4

            private const val TAG = "ParseExcelFileHelper"

        /**
         * Function for parsing schedule file
         * Firstly, find cell index for entered study group, then taking all classes for this group
         * Day of week, class number, start and end time of class, week number always takes from start of file
         * and if null takes last non-null value. For other fields take values raw be raw.
         * Lastly, return list with all classes for entered study group.
         *
         * @param inputStream input stream from schedule file
         * @param groupCode study group code
         *
         * @return List classes for the entered study group
         */

        suspend fun readFromExcel(inputStream: InputStream?, groupCode: String): MutableList<Class> {
                val classList = mutableListOf<Class>()
                Log.d(TAG, "readFromExcel")
                // Create a POI File System object
                val filePackage: OPCPackage?
                try {
                    filePackage = OPCPackage.open(inputStream)
                } catch (e: InvalidFormatException) {
                    inputStream?.close()
                    throw FileFormatException("Неверный формат файла")
                } catch (e2: RuntimeException) {
                    inputStream?.close()
                    throw FileFormatException("Неверный формат файла")
                }

                if(filePackage == null) {
                    Log.d(TAG, "filePackage is null")
                }
                //create workbook
                val workbook = XSSFWorkbook(filePackage)
                //get sheet
                val sheet = workbook.getSheet("Лист1")

                val groupRow = sheet.getRow(1)
                val cellIterator = groupRow.iterator()
                //find cell index for groupCode
                val cellIndex = findGroupIndex(cellIterator, groupCode)
                Log.d(TAG, "cellIndex = $cellIndex")

                if(cellIndex == -1) {
                    throw NoSuchElementException("Учебная группа не найдена")
                } else {
                    var dayOfWeek: String? = null
                    var classNumber: Int? = null
                    var classStartTime: String? = null
                    var classEndTime: String? = null
                    val rowIterator = sheet.iterator()
                    //move to 3 row , because first 3 rows are headers
                    rowIterator.next()
                    rowIterator.next()
                    rowIterator.next()

                    while (rowIterator.hasNext()) {
                        //check is current coroutine is active now
                        yield()
                        //changing value for this fields every time the value is non-null
                        //if value is null, take last non-null value(it saved)
                        val row = rowIterator.next()
                        //there is the number of class for each raw, so this value defines whether we've reached the end or not
                        val classWeek = getClassWeekNumOrNull(row) ?: break
                        Log.d(TAG, classWeek)

                        dayOfWeek =
                            getCellStringValue(
                                row,
                                DAY_OF_WEEK_COLUMN_INDEX,
                                dayOfWeek
                            )
                        classNumber =
                            getClassNumberValue(
                                row,
                                classNumber
                            )
                        classStartTime =
                            getCellStringValue(
                                row,
                                START_TIME__COLUMN_INDEX,
                                classStartTime
                            )
                        classEndTime =
                            getCellStringValue(
                                row,
                                END_TIME_COLUMN_INDEX,
                                classEndTime
                            )
                        //for next values: if value is null, then go to next raw
                        //reading values raw be raw.
                        var className: String? = null
                        if (row.getCell(cellIndex).cellTypeEnum == CellType.STRING) {
                            className = row.getCell(cellIndex).stringCellValue
                        } else if (row.getCell(cellIndex).cellTypeEnum == CellType.BLANK) {
                            continue
                        }
                        val classType =
                            row.getCell(cellIndex + 1).stringCellValue.toLowerCase(Locale.getDefault())
                        val classTeacherName =
                            row.getCell(cellIndex + 2).stringCellValue
                        val classAudience =
                            row.getCell(cellIndex + 3).stringCellValue

                        val clas = Class(
                            className,
                            classNumber,
                            classType,
                            classTeacherName,
                            classAudience,
                            dayOfWeek,
                            classStartTime,
                            classEndTime,
                            classWeek
                        )
                        Log.d(TAG, "$clas")
                        classList.add(clas)
                    }
                }

                workbook.close()
                inputStream?.close()
                filePackage.close()

                return classList
        }

        private fun findGroupIndex(
            cellIterator: MutableIterator<Cell>,
            groupCode: String
        ): Int {
            while(cellIterator.hasNext()) {
                val cell = cellIterator.next()
                if (cell.cellTypeEnum == CellType.STRING) {
                    if(cell.stringCellValue.startsWith(groupCode)) {
                        return cell.columnIndex
                    }
                }
            }
            return -1
        }

        private fun getClassWeekNumOrNull(row: Row): String? {
            val weekNumber =
                getCellStringValue(
                    row,
                    CLASS_WEEK_COLUMN_INDEX,
                    null
                ) ?: return null

            Log.d(TAG, "weekNumber is $weekNumber")
            return if(weekNumber == "I" || weekNumber == "II") weekNumber
            else null
        }

        private fun getCellStringValue(row: Row, parseColumnNum: Int, previousValue: String?): String? {
            val cell = row.getCell(parseColumnNum)
            return if (cell.cellTypeEnum == CellType.STRING) {
                cell.stringCellValue
            } else {
                previousValue
            }
        }

        private fun getClassNumberValue(row: Row, previousValue: Int?): Int? {
            val cell = row.getCell(CLASS_NUMBER_COLUMN_INDEX)
            return if (cell.cellTypeEnum == CellType.NUMERIC) {
                cell.numericCellValue.toInt()
            } else {
                previousValue
            }
        }
    }
}